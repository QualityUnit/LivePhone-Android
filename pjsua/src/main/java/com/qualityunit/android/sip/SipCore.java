/* $Id: MyApp.java 5102 2015-05-28 07:14:24Z riza $ */
/*
 * Copyright (C) 2013 Teluu Inc. (http://www.teluu.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.qualityunit.android.sip;

import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.StringVector;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjsip_transport_type_e;


public class SipCore {
    static {
//        try {
//            System.loadLibrary("openh264");
//            System.loadLibrary("yuv");
//        } catch (UnsatisfiedLinkError e) {
//            System.out.println("UnsatisfiedLinkError: " + e.getMessage());
//            System.out.println("This could be safely ignored if you " +
//                    "don't need video.");
//        }
        System.loadLibrary("pjsua2");
        System.out.println("Library loaded");
    }

    public static Endpoint ep;
    public static SipAppObserver observer;

    private EpConfig epConfig = new EpConfig();
    private TransportConfig sipTpConfig = new TransportConfig();

    // Maintain reference to log writer to avoid premature cleanup by GC
    private SipLogWriter logWriter;

    private final int SIP_PORT = 6001;
    private final int LOG_LEVEL = 4;

    public SipCore() {
        ep = new Endpoint();
    }

    public void init(SipAppObserver observer) throws Exception {
        init(observer, null);
    }

    public void init(SipAppObserver observer, String externalThread) throws Exception {
        this.observer = observer;
	    // Create endpoint
        ep.libCreate();
        sipTpConfig.setPort(SIP_PORT);

        // Override log level setting
        epConfig.getLogConfig().setLevel(LOG_LEVEL);
        epConfig.getLogConfig().setConsoleLevel(LOG_LEVEL);

	    // Set log config
        LogConfig log_cfg = epConfig.getLogConfig();
        logWriter = new SipLogWriter();
        log_cfg.setWriter(logWriter);
        log_cfg.setDecor(log_cfg.getDecor() &
                ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue()));

	    // Set ua config
        UaConfig ua_cfg = epConfig.getUaConfig();
        ua_cfg.setUserAgent("Pjsua2 Android " + ep.libVersion().getFull());
        StringVector stun_servers = new StringVector();
        stun_servers.add("stun.pjsip.org");
        ua_cfg.setStunServer(stun_servers);
//        if (own_worker_thread) {
//            ua_cfg.setThreadCnt(0);
//            ua_cfg.setMainThreadOnly(true);
//        }
        if (externalThread != null) {
            ep.libRegisterThread(externalThread);
        }
	    // Init endpoint
        ep.libInit(epConfig);

        // Create UDP transport
        ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig);

        // Create TCP transport (we need this to be enabled, because UDP supports requests up to 1300 bytes (e.g. hold request has more than 1300 bytes))
        ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, sipTpConfig);

	    // Start sip lib
        ep.libStart();
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
            ep.libDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

	/* Force delete Endpoint here, to avoid deletion from a non-
	* registered thread (by GC?). 
	*/
        ep.delete();

        ep = null;

    }
}
