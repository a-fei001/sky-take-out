package com.sky.task;


import com.sky.webSocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class WebSocketTask {
    //因为前端代码缺少错误处理，导致在这里可能对支付成功提醒造成影响，所以注释掉
//    @Autowired
//    private WebSocketServer webSocketServer;
//
//    /**
//     * 通过WebSocket每隔5秒向客户端发送消息
//     */
//    @Scheduled(cron = "0/5 * * * * ?")
//    public void sendMessageToClient() {
//        webSocketServer.sendToAllClient("这是来自服务端的消息：" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()));
//    }
}
