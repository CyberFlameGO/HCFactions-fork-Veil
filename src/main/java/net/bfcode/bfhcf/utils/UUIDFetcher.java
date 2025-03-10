package net.bfcode.bfhcf.utils;

import java.util.Iterator;
import org.json.simple.JSONObject;
import java.io.Reader;
import java.io.InputStreamReader;
import org.json.simple.JSONArray;
import java.util.HashMap;
import java.util.Collections;
import java.nio.ByteBuffer;
import java.net.URL;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Collection;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.json.simple.parser.JSONParser;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.Callable;

public class UUIDFetcher implements Callable<Map<String, UUID>>
{
    private static double PROFILES_PER_REQUEST = 100.0;
    private static String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private JSONParser jsonParser;
    private List<String> names;
    private boolean rateLimiting;
    
    public UUIDFetcher(List<String> names, boolean rateLimiting) {
        this.jsonParser = new JSONParser();
        this.names = (List<String>)ImmutableList.copyOf((Collection)names);
        this.rateLimiting = rateLimiting;
    }
    
    public UUIDFetcher(List<String> names) {
        this(names, true);
    }
    
    private static void writeBody(HttpURLConnection connection, String body) throws Exception {
        OutputStream stream = connection.getOutputStream();
        stream.write(body.getBytes());
        stream.flush();
        stream.close();
    }
    
    private static HttpURLConnection createConnection() throws Exception {
        URL url = new URL("https://api.mojang.com/profiles/minecraft");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }
    
    private static UUID getUUID(String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
    }
    
    public static byte[] toBytes(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }
    
    public static UUID fromBytes(byte[] array) {
        if (array.length != 16) {
            throw new IllegalArgumentException("Illegal byte array length: " + array.length);
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        long mostSignificant = byteBuffer.getLong();
        long leastSignificant = byteBuffer.getLong();
        return new UUID(mostSignificant, leastSignificant);
    }
    
    public static UUID getUUIDOf(String name) throws Exception {
        return new UUIDFetcher(Collections.singletonList(name)).call().get(name);
    }
    
    @Override
    public Map<String, UUID> call() throws Exception {
        HashMap<String, UUID> uuidMap = new HashMap<String, UUID>();
        for (int requests = (int)Math.ceil(this.names.size() / 100.0), i = 0; i < requests; ++i) {
            HttpURLConnection connection = createConnection();
            String body = JSONArray.toJSONString((List)this.names.subList(i * 100, Math.min((i + 1) * 100, this.names.size())));
            writeBody(connection, body);
            JSONArray array = (JSONArray)this.jsonParser.parse((Reader)new InputStreamReader(connection.getInputStream()));
            for (Object profile : array) {
                JSONObject jsonProfile = (JSONObject)profile;
                String id = (String)jsonProfile.get((Object)"id");
                String name = (String)jsonProfile.get((Object)"name");
                UUID uuid = getUUID(id);
                uuidMap.put(name, uuid);
            }
            if (this.rateLimiting) {
                if (i != requests - 1) {
                    Thread.sleep(100L);
                }
            }
        }
        return uuidMap;
    }
}
