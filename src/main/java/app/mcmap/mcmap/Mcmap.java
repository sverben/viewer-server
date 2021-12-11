package app.mcmap.mcmap;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;

public final class Mcmap extends JavaPlugin implements Listener {
    HttpServer web;

    @Override
    public void onEnable() {
        try {
            web = HttpServer.create(new InetSocketAddress(5767), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        web.createContext("/getChunk", new Handler());
        web.createContext("/getWorlds", new AllWorlds());
        web.setExecutor(null);
        web.start();
    }

    @Override
    public void onDisable() {
        web.stop(0);
    }

    class block {
        String block;
        int y;

        block(String block, int y) {
            this.block = block;
            this.y = y;
        }
    }

    public String chunkJson(int chunkX, int chunkZ, String worldName) {
        ArrayList<ArrayList> main = new ArrayList<>();
        World world = getServer().getWorld(worldName);
        if (world.getChunkAt(chunkX, chunkZ) == null) {
            return "[]";
        }

        for (int x = 0; x < 16; ++x) {
            ArrayList<block> second = new ArrayList<>();
            for (int z = 0; z < 16; ++z) {
                Block block = world.getHighestBlockAt(chunkX * 16 + x, chunkZ * 16 + z);
                second.add(new block(block.getType().name(), block.getY()));
            }

            main.add(second);
        }

        String json = new Gson().toJson(main);
        return json;
    }

    public String getQueryPart(URI url, String key) {
        String query = url.getQuery();
        if(query == null)
            return "0";

        String[] parts = query.split("[&=]");
        for(int i = 0; i < parts.length; i+=2) {
            if (parts[i].equalsIgnoreCase(key)) {
                return parts[i+1];
            }
        }

        return "0";
    }

    class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            URI url = t.getRequestURI();
            Integer x = Integer.parseInt(getQueryPart(url, "x"));
            Integer z = Integer.parseInt(getQueryPart(url, "z"));

            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String response = chunkJson(x, z, getQueryPart(url, "world"));
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    class AllWorlds implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            ArrayList<String> worlds = new ArrayList<>();

            for (World world : getServer().getWorlds()) {
                worlds.add(world.getName());
            }
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String response = new Gson().toJson(worlds);
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
