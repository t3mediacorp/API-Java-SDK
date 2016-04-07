package trackvia.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import trackvia.client.model.ApiError;
import trackvia.client.model.ApiErrorResponse;
import trackvia.client.model.RecordData;
import trackvia.client.model.RecordDataDeserializer;
import trackvia.client.model.VersionMisMatchExcpetionResponse;

public abstract class OverHttpCommand<T> {
	protected Gson gson;
	protected HttpClientContext context;
	protected TrackviaClient tvClient;
	
    public abstract HttpClientContext getContext();
    public abstract T execute(CloseableHttpClient httpClient);
    public abstract URI getApiRequestUri() throws URISyntaxException;
    public abstract T processResponseEntity(final HttpEntity entity) throws IOException;
    
    public OverHttpCommand(final HttpClientContext context, TrackviaClient tvClient) {
    	this.tvClient = tvClient;
        this.context = context;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .registerTypeAdapter(RecordData.class, new RecordDataDeserializer())
                .create();
    }
    
    
    /**
     * setup the headers
     * Mostly this just puts the API version in
     * @param request
     */
    protected void setHeaders(HttpRequestBase request){
    	if(tvClient.getApiVersion() == null){
    		return;
    	}
    	request.setHeader(TrackviaClient.API_VERSION_HEADER, tvClient.getApiVersion());
    }
    
    /**
     * Suck in a stream and turn it into a string
     * @param reader
     * @return
     */
    public static String slurp(final BufferedReader reader) {
        
        final StringBuilder out = new StringBuilder();
        String line;
        try{
	        while((line = reader.readLine()) != null){
	        	out.append(line);
	        	out.append("\n");
	        }
        } catch (IOException ex) {
        	out.append(ex.getMessage());
        }
        return out.toString();
    }
    
    /**
     * Handle the response from a HTTP request
     * @param validResponseCodes
     * @param response
     * @param uri
     * @param log
     * @return
     * @throws IOException
     */
    protected T handleResponse(CloseableHttpClient client, HttpRequestBase request, List<Integer> validResponseCodes, CloseableHttpResponse response, URI uri, Logger log) throws IOException{
    	T result = null;
    	int statusCode = response.getStatusLine().getStatusCode();
    	String badJsonStr = "Nothing went wrong first. This should never happen";
    	if (validResponseCodes.contains(statusCode)) {
    		result = processResponseEntity(response.getEntity());
    		log.debug("{} api response: {}", uri.getPath(), (result == null) ? ("none") : (result.toString()));
        } else if(response.getStatusLine().getStatusCode() == HttpStatus.SC_GONE){
        	ApiErrorResponse apiError = new ApiErrorResponse();
        	apiError.setError(ApiError.VersionMisMatch.code());
        	throw new TrackviaApiException(apiError);   	
        } else {
        	BufferedReader jsonReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            //mark the start incase it's an error and we can't read it
        	//errors should never be huge so hard code limit to 100k, we should never pass this.
        	jsonReader.mark(1024*1024);
            ApiErrorResponse apiError = null;
            try{
            	apiError = gson.fromJson(jsonReader, ApiErrorResponse.class);
            } catch(JsonSyntaxException badJson){
            	try{
            		badJsonStr = badJson.toString();
            		jsonReader.reset();
            		String errorStr = slurp(jsonReader);
            		apiError = new ApiErrorResponse();
            		apiError.setMessage("Error code: " + statusCode + " " + errorStr);
            		apiError.setError("Error code: " + statusCode + " " + errorStr);
            		throw new TrackviaApiException(apiError);
            	} catch (Exception t){
            		String whatWentWrong = "Something went wrong with an unknown error type: " + t.getMessage();
            		apiError = new ApiErrorResponse();
            		String errorStr = "Error code: " + statusCode + " --- " + whatWentWrong + "\nFirst thing that went wrong: " + badJsonStr;
            		apiError.setMessage(errorStr);
            		apiError.setError(errorStr);
            		throw new TrackviaApiException(apiError);
            	}
            }

            if(apiError == null){
            	apiError = new ApiErrorResponse();
            	apiError.setCode(response.getStatusLine().getStatusCode()+"");
            	apiError.setError(response.getStatusLine().getStatusCode()+"");
            	apiError.setMessage(response.getStatusLine().getReasonPhrase());
            }
            log.debug("{} api error: {}", uri.getPath(), apiError.toString());

            throw new TrackviaApiException(apiError);
        }	
    	return result;
    }
    
    /**
     * Handle version mismatch
     * @param response
     * @throws IllegalStateException
     * @throws IOException
     */
    protected void handleVersionMisMatch(CloseableHttpResponse response) throws IllegalStateException, IOException{
    	Reader jsonReader = new InputStreamReader(response.getEntity().getContent());
    	VersionMisMatchExcpetionResponse versionException = gson.fromJson(jsonReader, VersionMisMatchExcpetionResponse.class);
    	
        //need to retry
    }
}
