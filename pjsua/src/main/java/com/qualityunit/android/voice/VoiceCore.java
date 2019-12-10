package com.qualityunit.android.voice;

import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.StringVector;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjsip_transport_type_e;


public class VoiceCore {

    static {
        System.loadLibrary("pjsua2");
        System.out.println("Library loaded");
    }

    private Endpoint endpoint;

    private EpConfig epConfig;
    private TransportConfig sipTpConfig;

    // Maintain reference to log writer to avoid premature cleanup by GC
    private VoiceLogWriter logWriter;

    private static final int SIP_PORT = 5080;
    private static final int LOG_LEVEL = 4;

    public static VoiceCore create(String externalThread) throws Exception {
        VoiceCore instance = new VoiceCore();
        instance.init(externalThread);
        return instance;
    }

    public void init(String externalThread) throws Exception {
        epConfig = new EpConfig();
        sipTpConfig = new TransportConfig();

        endpoint = new Endpoint();

        // Create endpoint
        endpoint.libCreate();
        sipTpConfig.setPort(SIP_PORT);

        // Override log level setting
        epConfig.getLogConfig().setLevel(LOG_LEVEL);
        epConfig.getLogConfig().setConsoleLevel(LOG_LEVEL);

        // Set log config
        LogConfig log_cfg = epConfig.getLogConfig();
        logWriter = new VoiceLogWriter();
        log_cfg.setWriter(logWriter);
        log_cfg.setDecor(log_cfg.getDecor() &
                ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue()));

        // Set ua config
        UaConfig ua_cfg = epConfig.getUaConfig();
        ua_cfg.setUserAgent("Pjsua2 Android " + endpoint.libVersion().getFull());
        StringVector stun_servers = new StringVector();
        stun_servers.add("stun.pjsip.org");
        ua_cfg.setStunServer(stun_servers);
        if (externalThread != null) {
            endpoint.libRegisterThread(externalThread);
        }

        // Init endpoint
        endpoint.libInit(epConfig);

        // Create UDP transport
        endpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig);

        // Create TCP transport (we need this to be enabled, because UDP supports requests up to 1300 bytes (e.g. hold request has more than 1300 bytes))
        endpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, sipTpConfig);

        // Start sip lib
        endpoint.libStart();
    }

    protected Endpoint getEndpoint() {
        return this.endpoint;
    }

    public void deinit() {

        /* Try force GC to avoid late destroy of PJ objects as they should be
         * deleted before lib is destroyed.
         */
        Runtime.getRuntime().gc();

        /* Shutdown pjsua. Note that Endpoint destructor will also invoke
         * libDestroy(), so this will be a test of double libDestroy().
         */
        try {
            endpoint.libDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Force delete Endpoint here, to avoid deletion from a non-
         * registered thread (by GC?).
         */
        endpoint.delete();
        endpoint = null;

    }
}

