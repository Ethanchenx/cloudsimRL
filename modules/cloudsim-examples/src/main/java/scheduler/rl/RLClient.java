package scheduler.rl;


/**
 * @Author: Chen
 * @File Name: RLClient.java
 */


import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class RLClient {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Gson gson = new Gson();

    public RLClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public int getAction(List<Double> state, int cloudletId) throws IOException, InterruptedException {
        Thread.sleep(50);
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (double v : state) arr.add(v);
        obj.add("state", arr);
        obj.addProperty("cloudletId", cloudletId);

        // 发送 JSON 请求
        out.write(obj.toString());
        out.newLine();
        out.flush();
        System.out.println("Sending State: " + obj.toString());


        // 接收返回 JSON
        String response = in.readLine();
        JsonObject result = gson.fromJson(response, JsonObject.class);
        return result.get("action").getAsInt();
    }

    public void sendReward(List<Double> nextState, Double reward) throws IOException, InterruptedException {
        Thread.sleep(50);
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        obj.addProperty("reward", reward);
        for (double v : nextState) arr.add(v);
        obj.add("next_state", arr);


        // 发送 JSON 请求
        out.write(obj.toString());
        out.newLine();
        out.flush();
        System.out.println("Sending Reward: " + obj.toString());

    }

    public void close() throws IOException {
        socket.close();
        in.close();
        out.close();
    }
}
