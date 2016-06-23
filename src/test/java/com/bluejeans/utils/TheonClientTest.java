/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluejeans.utils.theon.TheonClient;

/**
 * test for theon client
 *
 * @author Dinesh Ilindra
 */
public class TheonClientTest {

    // @Test
    public void testTheonClient() throws Exception {
        final TheonClient<String> theonClient = new TheonClient<String>("http://10.5.7.193:3000/v2", "theon", "greyjoy");
        theonClient.setFileBasedQueue(true);
        theonClient.setPeekEnabled(true);
        theonClient.setQueueDir("/tmp/filequeue/theontest");
        theonClient.setQueueName("clienttest");
        // final TheonClient<String> theonClient = new
        // TheonClient<String>("http://10.5.7.65:3000", "indigo", "indigo");
        // theonClient.setGzipEnabled(true);
        theonClient.setBulkPollIntervalSecs(1);
        theonClient.setQueueCapacity(5);
        theonClient.setMaxPostEntitySize(10);
        theonClient.init();
        theonClient.postMessagesNow("testagain", "123", "testing1\ntestagain1", "testing2", "testing3");
        System.out.println(theonClient.getTheonCounter().getEventCounts());
        theonClient.setMaxPostEntitySize(100);
        theonClient.postMessagesNow("testagain", "456", "testing1\ntestagain1", "testing2", "testing3");
        System.out.println(theonClient.getTheonCounter().getEventCounts());
        theonClient.setMaxPostEntitySize(10240);
        final List<String> mlist = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            mlist.add("testing" + i);
        }
        theonClient.postMessagesNow("indigo1", "trew", mlist);
        final String oneMsg = "{\"endpointStartTime\":1447910963000,\"id\":{\"endpointId\":18123335,\"timestamp\":1447911354000},\"adjectivizedProperties\":[{\"adjectives\":[],\"values\":{\"isAudioRecv\":false,\"isVideoRecv\":false,\"callQuality\":2,\"callQualitySend\":0,\"callQualityRecv\":2,\"isCurrentPresenter\":false,\"isEchoSuppressed\":false,\"layout\":47,\"roundTripDelay\":250,\"isTalkDetected\":true,\"isVideoLipSyncWarning\":false,\"esmStatus\":false,\"ecBufferDelay\":0,\"ecBufferedData\":0,\"echoDetected\":0.0,\"echoPresent\":0.0,\"echoRemoved\":0.0,\"xcorr\":0.0,\"rfactor\":0.22796583,\"processCpuLoad\":94,\"systemCpuLoad\":100,\"cpuClockRate\":0,\"wifiRecvStrength\":2,\"echoBufferStarvationEvent\":1,\"speakingWhileMutedEvent\":1,\"localEchoProbableEvent\":1,\"highCpuLoadEvent\":1,\"rxBWProblemEvent\":1,\"txBWProblemEvent\":1}},{\"adjectives\":[\"audio\",\"recv\"],\"values\":{\"isRemoteMute\":true,\"isLocalMute\":true}},{\"adjectives\":[\"video\",\"recv\"],\"values\":{\"isRemoteMute\":false,\"isLocalMute\":false}},{\"adjectives\":[\"video\",\"send\"],\"values\":{\"codec\":\"H.264\",\"height\":1,\"width\":65}},{\"adjectives\":[\"audio\",\"inbound\",\"rtp\"],\"values\":{\"rtpChannelId\":0,\"media\":0,\"direction\":0,\"mixerTime\":20.0,\"channelQuality\":20,\"numPackets\":174723,\"numBytes\":31016524,\"bitrate\":398,\"processTime\":20.0,\"loss\":0.0,\"jitter\":0.98453456,\"cumulLoss\":3,\"bufferTime\":66,\"channelType\":0,\"isContent\":false,\"timestamp\":1447911354000,\"popLoss\":0.9811304,\"popCumulLoss\":0,\"popJitter\":0.92360884,\"popMaxJitter\":0.78286564,\"avgLoss\":0.0,\"popAvgLoss\":0.0,\"networkCumulLoss\":5090,\"networkLoss\":82.42837,\"networkAvgLoss\":64.61693,\"droppedPackets\":0,\"maxJitter\":0.8778765,\"agcGain\":0.96236616,\"snr\":0.6303787,\"vadPercent\":0.60809815,\"mixUnderRuns\":327025184,\"avgMixQueueSize\":10,\"minMixQueueSize\":5,\"maxMixQueueSize\":20}},{\"adjectives\":[\"audio\",\"outbound\",\"rtp\"],\"values\":{\"rtpChannelId\":1,\"media\":0,\"direction\":1,\"mixerTime\":20.0,\"channelQuality\":20,\"numPackets\":36693,\"numBytes\":13807538,\"bitrate\":548,\"processTime\":20.0,\"loss\":0.0,\"jitter\":0.96828705,\"cumulLoss\":3,\"bufferTime\":57,\"channelType\":0,\"isContent\":false,\"timestamp\":1447911354000,\"popLoss\":0.8629016,\"popCumulLoss\":0,\"popJitter\":0.93231755,\"popMaxJitter\":0.9519798,\"avgLoss\":0.0,\"popAvgLoss\":0.0,\"networkCumulLoss\":860,\"networkLoss\":98.312645,\"networkAvgLoss\":85.53679,\"roundTripDelay\":927}},{\"adjectives\":[\"video\",\"inbound\",\"rtp\"],\"values\":{\"rtpChannelId\":2,\"media\":1,\"direction\":0,\"mixerTime\":20.0,\"channelQuality\":20,\"numPackets\":34502,\"numBytes\":12071550,\"bitrate\":571,\"processTime\":20.0,\"loss\":0.0,\"jitter\":0.8789632,\"cumulLoss\":2,\"bufferTime\":52,\"channelType\":0,\"isContent\":false,\"timestamp\":1447911354000,\"popLoss\":0.72440046,\"popCumulLoss\":0,\"popJitter\":0.91264665,\"popMaxJitter\":0.9031959,\"avgLoss\":0.0,\"popAvgLoss\":0.0,\"networkCumulLoss\":9954,\"networkLoss\":99.025055,\"networkAvgLoss\":82.96696,\"fps\":16.0,\"keyFrameRate\":0.0,\"droppedPackets\":0,\"maxJitter\":0.9994836}},{\"adjectives\":[\"video\",\"outbound\",\"rtp\"],\"values\":{\"rtpChannelId\":3,\"media\":1,\"direction\":1,\"mixerTime\":20.0,\"channelQuality\":20,\"numPackets\":59555,\"numBytes\":115057984,\"bitrate\":686,\"processTime\":20.0,\"loss\":0.0,\"jitter\":0.9819151,\"cumulLoss\":3,\"bufferTime\":58,\"channelType\":0,\"isContent\":false,\"timestamp\":1447911354000,\"popLoss\":0.9090324,\"popCumulLoss\":0,\"popJitter\":0.87864375,\"popMaxJitter\":0.95630014,\"avgLoss\":0.0,\"popAvgLoss\":0.0,\"networkCumulLoss\":5152,\"networkLoss\":92.45581,\"networkAvgLoss\":77.74069,\"fps\":12.0,\"keyFrameRate\":0.0,\"roundTripDelay\":940}},{\"adjectives\":[\"content\",\"inbound\",\"rtp\"],\"values\":{\"rtpChannelId\":4,\"media\":1,\"direction\":0,\"mixerTime\":20.0,\"channelQuality\":20,\"numPackets\":121000,\"numBytes\":134323136,\"bitrate\":610,\"processTime\":20.0,\"loss\":0.0,\"jitter\":0.9696603,\"cumulLoss\":2,\"bufferTime\":43,\"channelType\":0,\"isContent\":true,\"timestamp\":1447911354000,\"popLoss\":0.8796924,\"popCumulLoss\":0,\"popJitter\":0.82089484,\"popMaxJitter\":0.8910935,\"avgLoss\":0.0,\"popAvgLoss\":0.0,\"fps\":15.0,\"keyFrameRate\":0.0,\"droppedPackets\":0,\"maxJitter\":0.9676938}},{\"adjectives\":[\"content\",\"outbound\",\"rtp\"],\"values\":{\"rtpChannelId\":5,\"media\":1,\"direction\":1,\"mixerTime\":20.0,\"channelQuality\":20,\"numPackets\":98844,\"numBytes\":140248688,\"bitrate\":326,\"processTime\":20.0,\"loss\":0.0,\"jitter\":0.93298805,\"cumulLoss\":0,\"bufferTime\":66,\"channelType\":0,\"isContent\":true,\"timestamp\":1447911354000,\"popLoss\":0.9746968,\"popCumulLoss\":0,\"popJitter\":0.94884557,\"popMaxJitter\":0.9315977,\"avgLoss\":0.0,\"popAvgLoss\":0.0,\"fps\":13.0,\"keyFrameRate\":0.0,\"roundTripDelay\":858}}],\"timestamp\":1447911354000,\"endpointId\":18123335}";
        final List<String> omlist = new ArrayList<String>();
        final Map<String, List<String>> omMap = new HashMap<String, List<String>>();
        for (int i = 0; i < 10; i++) {
            omlist.add(oneMsg);
            omMap.put("newkey" + i, omlist);
        }
        theonClient.postMessagesNow("indigo1", "qwerty", omlist);
        theonClient.postMessagesNow("indigo1", "", omMap);
        System.out.println(theonClient.getTheonCounter().getEventCounts());
        for (int i = 0; i < 20; i++) {
            theonClient.postMessage("test", "", "testing" + i, true);
        }
        Thread.sleep(2000);
        System.out.println(theonClient.getTheonCounter().getEventCounts());
        System.out.println(theonClient.getParallelBulkOperationUtil().getQueueAddFailCount());
        theonClient.destroy();
    }

    public static void main(final String[] args) throws Exception {
        new TheonClientTest().testTheonClient();
    }
}
