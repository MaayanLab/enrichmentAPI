package cloudinteraction;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

public class AmazonAWS {
	
	public static void main(String[] args) throws IOException {
       AmazonAWS aws = new AmazonAWS();
       
       for(int i=0; i<args.length; i++) {
    	   if(args[i].equals("-f")) {
    		   String[] sp = args[i+1].split("/");
    		   uploadS3("mssm-sigcomm", args[i+1], sp[sp.length-1]);
    	   }
    	   
    	   if(args[i].equals("-d")) {
    		   downloadS3("mssm-sigcomm", args[i+1], args[i+1]);
    	   }
       }
    }
	
	public static AmazonS3 getS3Client() {
		
		AmazonS3 s3Client = null;
		
		try {
			// first try to load user environmental variables, if not existent try to load S3 client with role
			String aws_key = System.getenv("AWS_ACCESS_KEY_ID");
			String aws_endpoint_url = System.getenv("AWS_ENDPOINT_URL");
			if (aws_endpoint_url == null) {
				aws_endpoint_url = "https://s3.us-east-1.amazonaws.com";
			}

			if (aws_key != null) {
				System.out.println("Using user credentials");
				// load client using password
				
				BasicAWSCredentials awsCreds = new BasicAWSCredentials(aws_key, System.getenv("AWS_SECRET_ACCESS_KEY"));
				
				Regions region = Regions.fromName("us-east-1");

				s3Client = AmazonS3ClientBuilder.standard()
					.withEndpointConfiguration(new EndpointConfiguration(aws_endpoint_url, region.getName()))
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
					.build();
			}
			else {
				// only works when running an EC2 instance with correct role attached
				System.out.println("Using role credentials");
				s3Client = AmazonS3ClientBuilder.standard()
					.withCredentials(new InstanceProfileCredentialsProvider(false))
					.build();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return s3Client;
	}
	
	public static void uploadS3(String _bucket, String _filePath, String _key) {
		
        String bucketName = _bucket;
        String keyName = _key;
        String filePath = _filePath;
        
        File file = new File(filePath);
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB. 

        try {
            AmazonS3 s3Client = getS3Client();
            
            // Create a list of ETag objects. You retrieve ETags for each object part uploaded,
            // then, after each individual part has been uploaded, pass the list of ETags to 
            // the request to complete the upload.
            List<PartETag> partETags = new ArrayList<PartETag>();

            // Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

            // Upload the file parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                // Because the last part could be less than 5 MB, adjust the part size as needed.
                partSize = Math.min(partSize, (contentLength - filePosition));

                // Create the request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucketName)
                        .withKey(keyName)
                        .withUploadId(initResponse.getUploadId())
                        .withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(partSize);

                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());

                filePosition += partSize;
            }

            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, keyName,
                    initResponse.getUploadId(), partETags);
            s3Client.completeMultipartUpload(compRequest);
        }
        catch(AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process 
            // it, so it returned an error response.
            e.printStackTrace();
        }
        catch(SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }
	}
	
	public static void downloadS3(String _bucket, String _filename, String _outfile) {
		
		System.out.println("------------ Download -------------");
		System.out.println("Bucket: "+_bucket);
		System.out.println("File: "+_filename);
		System.out.println("Outfile: "+_outfile);
		
		AmazonS3 s3Client = getS3Client();

		S3Object object = s3Client.getObject(new GetObjectRequest(_bucket, _filename));

		try {
			InputStream reader = new BufferedInputStream(object.getObjectContent());
			File file = new File(_outfile);      
			OutputStream writer = new BufferedOutputStream(new FileOutputStream(file));

			int read = -1;

			while ( ( read = reader.read() ) != -1 ) {
			    writer.write(read);
			}
			
			writer.flush();
			writer.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
