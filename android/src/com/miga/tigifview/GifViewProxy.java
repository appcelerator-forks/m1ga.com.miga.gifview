package com.miga.gifview;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.kroll.KrollProxy;
import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.appcelerator.titanium.TiBlob;
import com.felipecsl.gifimageview.library.GifImageView;
import org.appcelerator.titanium.view.TiDrawableReference;
import android.graphics.BitmapFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedInputStream;
import java.net.URLConnection;
import android.graphics.Bitmap;

@Kroll.proxy(creatableInModule = TigifviewModule.class)
public class GifViewProxy extends TiViewProxy {

    GifImageView gifView;
    TiApplication appContext;
    Activity activity;
    String imageSrc;
    private static TiBaseFile file;
    private boolean autoStart = false;
    public static final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
    private static TiBlob imgObj = null;

	public GifViewProxy() {
		super();
		appContext = TiApplication.getInstance();
		activity   = appContext.getCurrentActivity();
	}
    


	@Kroll.method
    public void stop() {
        if (gifView != null) {
            gifView.stopAnimation();
        }
    }

    @Kroll.method
    public void start() {
        if (gifView != null) {
            gifView.startAnimation();
        }
    }

    @Kroll.getProperty @Kroll.method
    public String getImage() {
        return imageSrc;      
    }

    @Kroll.setProperty @Kroll.method
    public void setImage(String url) {
        if (gifView != null) {
            gifView.stopAnimation();
        }
        imageSrc = url;
        openImage();          
    }

    @Kroll.setProperty @Kroll.method
    public void setAutoStart(boolean val) {
        autoStart = val;       
    }
    
    @Kroll.getProperty @Kroll.method
    public boolean getAutoStart() {
        return autoStart;       
    }
    
    @Override
    public TiUIView createView(Activity activity) {
        TiUIView view = new GifView(this);
        return view;
    }

    private String getPathToApplicationAsset(String assetName)
    	{
    		// The url for an application asset can be created by resolving the specified
    		// path with the proxy context. This locates a resource relative to the 
    		// application resources folder
    		
    		String result = resolveUrl(null, assetName);
    		return result;
    	}
        
    // Handle creation options
	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);

		if (options.containsKey("image")) {
			imageSrc = options.getString("image");          
		}
		if (options.containsKey("autoStart")) {
			autoStart = options.getBoolean("autoStart");          
		}

	}


    public byte[] readBytes(InputStream inputStream) throws IOException {
        // http://stackoverflow.com/a/2436413/5193915
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        
        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }
    
    public byte[] getRemoteImage(final URL aURL) {
        try {
            final URLConnection conn = aURL.openConnection();
            conn.connect();
            final BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len = 0;
            while ((len = bis.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            bis.close();

            return byteBuffer.toByteArray();            
        } catch (IOException e) {
            Log.e("round","Error fetching url");
        }
        return null;
    }
    
    private void openImage(){
        
        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(imageSrc);//replace with string to compare

        if(m.find()) {                
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        if (gifView != null) {
                            Log.i("V",getRemoteImage(new URL(imageSrc))  + "" );
                            gifView.setBytes( getRemoteImage(new URL(imageSrc)) );
                            
                            if (autoStart){
                                gifView.startAnimation();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("round","REMOTE error" + e);
                        e.printStackTrace();
                    }
                }
            });
            thread.start();        
        } else {
            String url = getPathToApplicationAsset(imageSrc);
            TiBaseFile file = TiFileFactory.createTitaniumFile(new String[] { url }, false);            
             
            try {
                if (file!=null) {
                    InputStream is = file.getInputStream();
                    if (gifView != null) {
                        gifView.setBytes(readBytes(is));
                        if (autoStart){
                            gifView.startAnimation();
                        }
                    } else {
                        Log.e("GIF","View not found");    
                    }
                } else {
                    Log.e("GIF","File is null");
                }
            } catch (IOException e){
                
            }
        }
    }
    
    private class GifView extends TiUIView {
        // create view
        public GifView(final TiViewProxy proxy) {
            super(proxy);
            String packageName = proxy.getActivity().getPackageName();
            Resources resources = proxy.getActivity().getResources();
            View videoWrapper;
            int resId_videoHolder = -1;
            int resId_video       = -1;

            resId_videoHolder = resources.getIdentifier("layout", "layout", packageName);
            resId_video       = resources.getIdentifier("gifImageView", "id", packageName);
            
            LayoutInflater inflater     = LayoutInflater.from(getActivity());
            videoWrapper = inflater.inflate(resId_videoHolder, null);
            gifView   = (GifImageView)videoWrapper.findViewById(resId_video);
            
            openImage();
            setNativeView(videoWrapper);
        }

        @Override
        public void processProperties(KrollDict d) {
            super.processProperties(d);
        }

    }

}
