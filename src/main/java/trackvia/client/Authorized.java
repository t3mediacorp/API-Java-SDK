package trackvia.client;

import trackvia.client.model.ApiError;

import java.util.concurrent.Callable;

public class Authorized<V> {
    private TrackviaClient client;

    public Authorized(final TrackviaClient client) {
        this.client = client;
    }

    public V execute(final Callable<V> callable) {
        boolean tryExecute = true;
        boolean tryOnceTokenRefresh = false;
        boolean tryOnceVersionMisMatch = false;
        V result = null;

        while (tryExecute) {
            try {
                if (tryOnceTokenRefresh || tryOnceVersionMisMatch) client.refreshAccessToken();

                result = callable.call();

                tryExecute = false;
            } catch (TrackviaApiException e) {
                ApiError err = e.getApiError();
                if ((err == ApiError.InvalidGrant || err == ApiError.InvalidToken) && !tryOnceTokenRefresh) {
                    tryOnceTokenRefresh = true;
                } else if(err != null 
                		&& err.code() != null 
                		&& err.code().equals(ApiError.VersionMisMatch.code()) 
                		&& !tryOnceVersionMisMatch){
                	tryOnceVersionMisMatch = true;
                	
                } else {
                    throw e;
                }
            } catch (Exception e) {
                throw new TrackviaClientException(e);
            }
        }

        return result;
    }
}
