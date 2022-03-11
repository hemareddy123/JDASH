package cam;

import config.ConfigManager;
import dash.DashManager;
import dash.dynamic.PreProcessMediaManager;
import dash.dynamic.message.PreLiveMediaProcessRequest;
import dash.dynamic.message.base.MessageHeader;
import dash.dynamic.message.base.MessageType;
import network.definition.DestinationRecord;
import network.socket.GroupSocket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.AppInstance;
import service.ServiceManager;
import util.module.FileManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraService {

    ////////////////////////////////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(CameraService.class);

    private final ConfigManager configManager;

    protected static final int CAMERA_INDEX = 0;
    protected static final int MIKE_INDEX = 4;

    protected FrameGrabber grabber;
    public final double FRAME_RATE = 30;
    public static final int CAPTURE_WIDTH = 1280;
    public static final int CAPTURE_HEIGHT = 720;
    public static final int GOP_LENGTH_IN_FRAMES = 30;
    private final String URI;

    private final OpenCVFrameConverter.ToIplImage openCVConverter = new OpenCVFrameConverter.ToIplImage();
    private FFmpegFrameRecorder fFmpegFrameRecorder = null;
    private final AudioService audioService = new AudioService();

    private static long startTime = 0;
    private boolean alive = true;
    private boolean isPreMediaReqSent = false;
    ////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////
    public CameraService() {
        configManager = AppInstance.getInstance().getConfigManager();
        //URI = FileManager.concatFilePath(configManager.getCameraMp4Path(), "cam_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".mp4");
        String networkPath = "rtmp://" + configManager.getRtmpPublishIp() + ":" + configManager.getRtmpPublishPort();
        URI = FileManager.concatFilePath(networkPath, configManager.getCameraPath());
    }
    ////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////
    public void initOutput() throws Exception {
        fFmpegFrameRecorder = new FFmpegFrameRecorder(
                URI,
                CAPTURE_WIDTH,
                CAPTURE_HEIGHT,
                AudioService.CHANNEL_NUM
        );

        fFmpegFrameRecorder.setVideoOption("tune", "zerolatency");
        fFmpegFrameRecorder.setVideoOption("preset", "ultrafast");
        fFmpegFrameRecorder.setVideoOption("crf", "28");
        fFmpegFrameRecorder.setVideoBitrate(2000000);

        fFmpegFrameRecorder.setFormat("flv"); // > H264
        fFmpegFrameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        fFmpegFrameRecorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        //fFmpegFrameRecorder.setFormat("matroska"); // > H265
        //fFmpegFrameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H265);

        fFmpegFrameRecorder.setGopSize(GOP_LENGTH_IN_FRAMES);
        fFmpegFrameRecorder.setFrameRate(FRAME_RATE);

        audioService.setRecorderParams(fFmpegFrameRecorder);
        audioService.initSampleService();

        fFmpegFrameRecorder.start();
        audioService.startSampling(FRAME_RATE);
        ///////////////////////////////////////////////
    }

    public void releaseOutputResource() throws Exception {
        audioService.releaseOutputResource();
        fFmpegFrameRecorder.close();
    }

    protected void initGrabber() throws Exception {
        grabber = new OpenCVFrameGrabber(CAMERA_INDEX);
        grabber.setImageWidth(CAPTURE_WIDTH);
        grabber.setImageHeight(CAPTURE_HEIGHT);
        grabber.start();
    }

    private void process() {
        try {
            final CanvasFrame cameraFrame = new CanvasFrame("[LOCAL] Live stream", CanvasFrame.getDefaultGamma() / grabber.getGamma());

            Frame capturedFrame;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curData = new Date();

            Point point = new Point(15, 35);
            Scalar scalar = new Scalar(0, 200, 255, 0);

            while ((capturedFrame = grabber.grab()) != null) {
                Mat mat = openCVConverter.convertToMat(capturedFrame);
                if (mat != null) {
                    curData.setTime(System.currentTimeMillis());
                    opencv_imgproc.putText(mat, simpleDateFormat.format(curData), point, opencv_imgproc.CV_FONT_VECTOR0, 0.8, scalar, 1, 0, false);
                    capturedFrame = openCVConverter.convert(mat);
                }

                if (alive && cameraFrame.isVisible()) {
                    cameraFrame.showImage(capturedFrame);
                }

                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                } else {
                    if (!isPreMediaReqSent) {
                        if ((System.currentTimeMillis() - startTime) >= configManager.getPreprocessInitIdleTime()) { // 5초 후에 PLAY 전송
                            sendPreLiveMediaProcessRequest();
                            isPreMediaReqSent = true;
                        }
                    }
                }

                // Check for AV drift
                long videoTS = 1000 * (System.currentTimeMillis() - startTime);
                if (videoTS > fFmpegFrameRecorder.getTimestamp()) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Lip-flap correction: [{}] : [{}] -> [{}]",
                                videoTS, fFmpegFrameRecorder.getTimestamp(),
                                (videoTS - fFmpegFrameRecorder.getTimestamp())
                        );
                    }
                    fFmpegFrameRecorder.setTimestamp(videoTS);
                }

                if (alive) {
                    fFmpegFrameRecorder.record(capturedFrame);
                }
            }
        } catch (Exception e) {
            FFmpegLogCallback.set();
            logger.warn("CameraService.process.Exception", e);
        }
    }

    private void safeRelease() {
        try {
            releaseOutputResource();
        } catch (Exception e) {
            logger.error("CameraService.safeRelease.Exception", e);
        }

        if (grabber != null) {
            try {
                grabber.close();
            } catch (Exception e) {
                logger.error("CameraService.safeRelease.Exception", e);
            }
        }
    }

    private void init() throws Exception {
        avutil.av_log_set_level(avutil.AV_LOG_INFO);
        FFmpegLogCallback.set();
        initGrabber();
        initOutput();
    }

    public void action() {
        try {
            init();
            process();
        } catch (Exception e) {
            logger.error("CameraService.action.Exception", e);
        } finally {
            safeRelease();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    private void sendPreLiveMediaProcessRequest () {
        DashManager dashManager = ServiceManager.getInstance().getDashManager();
        PreProcessMediaManager preProcessMediaManager = dashManager.getPreProcessMediaManager();
        GroupSocket listenSocket = preProcessMediaManager.getLocalGroupSocket();
        if (listenSocket != null) {
            DestinationRecord target = listenSocket.getDestination(preProcessMediaManager.getSessionId());
            if (target != null) {
                ConfigManager configManager = AppInstance.getInstance().getConfigManager();

                PreLiveMediaProcessRequest preLiveMediaProcessRequest = new PreLiveMediaProcessRequest(
                        new MessageHeader(
                                PreProcessMediaManager.MESSAGE_MAGIC_COOKIE,
                                MessageType.PREPROCESS_REQ,
                                dashManager.getPreProcessMediaManager().getRequestSeqNumber().getAndIncrement(),
                                System.currentTimeMillis(),
                                PreLiveMediaProcessRequest.MIN_SIZE + configManager.getCameraPath().length()
                        ),
                        configManager.getPreprocessListenIp().length(),
                        configManager.getPreprocessListenIp(),
                        configManager.getCameraPath().length(),
                        configManager.getCameraPath(),
                        1800
                );
                byte[] requestByteData = preLiveMediaProcessRequest.getByteData();
                target.getNettyChannel().sendData(requestByteData, requestByteData.length);
                logger.debug("[CameraService] SEND PreLiveMediaProcessRequest={}", preLiveMediaProcessRequest);
            }
        }
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
    ////////////////////////////////////////////////////////////////////////////////

}