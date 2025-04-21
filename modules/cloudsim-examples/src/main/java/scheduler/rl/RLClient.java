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

    public int getAction(List<Double> state, Long cloudletLength, int cloudletId, List<Double> estimateRuntime) throws IOException, InterruptedException {
//        Thread.sleep(20);
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (double v : state) arr.add(v);
        obj.add("state", arr);
        obj.addProperty("cloudletLength", cloudletLength);
        obj.addProperty("cloudletId", cloudletId);
        JsonArray arr1 = new JsonArray();
        for (double v : estimateRuntime) arr1.add(v);
        obj.add("estimateRuntime", arr1);

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

    public void sendConfig(int vm_nums, int task_nums, int iteration_nums, String dataset_name) throws IOException, InterruptedException {
        Thread.sleep(50);
        JsonObject obj = new JsonObject();
        obj.addProperty("vm_nums", vm_nums);
        obj.addProperty("task_nums", task_nums);
        obj.addProperty("iteration_nums", iteration_nums);
        obj.addProperty("dataset_name", dataset_name);



        // 发送 JSON 请求
        out.write(obj.toString());
        out.newLine();
        out.flush();
        System.out.println("Sending Configs: " + obj.toString());

    }

    public void close() throws IOException {
        socket.close();
        in.close();
        out.close();
    }
}
