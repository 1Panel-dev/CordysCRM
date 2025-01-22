package io.cordys.crm.system.notice.sse;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@WebServlet("/sse")
public class SseServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 设置 HTTP 头部
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

        // 响应流
        PrintWriter out = response.getWriter();

        // 定时发送消息
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 发送消息，消息格式为: "data: [message]\n\n"
                out.write("data: " + new Date().toString() + "\n\n");
                out.flush();
            }
        }, 0, 2000); // 每秒发送一次

        // 保持连接直到客户端关闭
        try {
            while (!Thread.interrupted()) {
                Thread.sleep(10000); // 保持连接
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
