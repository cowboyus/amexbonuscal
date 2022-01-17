import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;

public class Starter extends JFrame {
    private JTextArea pasteFetchRequestOfTextArea;
    private JButton doItButton;
    private JLabel label;
    private JLabel label2;
    private JComboBox<Integer> comboBox1;
    private JComboBox<Integer> comboBox2;
    private JPanel panel;
    private JTextArea pasteCookieHereTextArea;
    private JTextArea resultTextArea;

    public Starter() {
        panel.setPreferredSize(new Dimension(700, 700));
        setContentPane(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);

        comboBox1.setModel(new DefaultComboBoxModel<>(new Integer[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24}));
        comboBox1.setSelectedIndex(3);
        comboBox2.setModel(new DefaultComboBoxModel<>(new Integer[]{500,1000,2000,5000,10000,20000,50000}));
        comboBox2.setSelectedIndex(2);
        doItButton.addActionListener(e -> {
            String text = pasteFetchRequestOfTextArea.getText();
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create("https://functions.americanexpress.com/ReadLoyaltyTransactions.v1"));
            JSONObject jsonObject = new JSONObject(text.substring(text.indexOf("{"), text.lastIndexOf("}")+1));
            JSONObject headers = jsonObject.getJSONObject("headers");
            for (int i = 0; i < headers.length(); i++) {
                String key = headers.keys().next();
                request.setHeader(key, headers.getString(key));
            }
            request.setHeader("cookie", pasteCookieHereTextArea.getText());

            TreeMap<String, Double> map = new TreeMap<>();

            for (int i = 0; i < comboBox1.getSelectedIndex(); i++) {
                request.POST(HttpRequest.BodyPublishers.ofString(requestBody(jsonObject, i)));
                HttpClient client = HttpClient.newHttpClient();
                client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenAccept(response -> {
                            JSONObject res = new JSONObject(response);
                            Object transactions = res.get("transactions");
                            if (transactions instanceof JSONArray) {
                                JSONArray jsonArray = (JSONArray) transactions;
                                for(int j = 0; j < jsonArray.length(); j++) {
                                    JSONObject object = jsonArray.getJSONObject(j);
                                    Object des = object.get("descriptions");
                                    Object value = object.get("rewardAmount");
                                    if (des instanceof String && value instanceof JSONObject) {
                                        if (!map.containsKey(des)) {
                                            map.put((String) des, 0.0);
                                        }
                                        map.put((String) des, map.get((String) des) + ((JSONObject) value).getDouble("value"));
                                    }
                                }
                            }
                        })
                        .join();
            }

            List<Map.Entry<String, Double>> sorted = new ArrayList<>(map.entrySet());
            sorted.sort((o1, o2) -> {
                if (o1.getValue() - o2.getValue() > 0) {
                    return -1;
                } else if (o1.getValue() - o2.getValue() < 0) {
                    return 1;
                }
                return 0;
            });

            StringBuilder output = new StringBuilder();
            double grandTotal = 0.0;
            for (Map.Entry<String, Double> entry : sorted) {
                if (entry.getValue() > 0 ) {
                    grandTotal += entry.getValue();
                }
                if (entry.getValue() >= (int) comboBox2.getSelectedItem()) {
                    output.append(entry+ "\n");
                }
            }
            resultTextArea.setText("Grand Total: " + grandTotal + "\n" + output);
        });
    }

    private String requestBody(JSONObject fetch, int period) {
        return fetch.getString("body").replaceFirst("\"limit\":[0-9]+", "\"limit\":9999")
                .replaceFirst("\"periodIndex\":[0-9]+", "\"periodIndex\":"+period);
    }

    public static void main(String[] args) {
        Starter starter = new Starter();
        starter.pack();
    }
}
