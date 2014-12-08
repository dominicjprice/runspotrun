package uk.ac.horizon.runspotrun.net;

import org.alexd.jsonrpc.JSONRPCClient;
import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCParams;

import uk.ac.horizon.runspotrun.app.Urls;

public class JsonRpc {

	private final JSONRPCClient client;
	
	public JsonRpc(String accessToken) {
		client = JSONRPCClient.create(
				Urls.JSON_RPC(accessToken),
				JSONRPCParams.Versions.VERSION_2);
	}
	
	public boolean dummy()
	throws JSONRPCException {
		return client.callBoolean("marathon.dummy");	
	}

	public String requestVideoUploadUrl(String guid)
	throws JSONRPCException
	{
		return client.callString("marathon.request_video_upload_url", guid);
	}
	
	public void submitVideoEncodingJob(String guid)
	throws JSONRPCException {
		client.call("marathon.submit_video_encoding_job", guid);
	}
	
}
