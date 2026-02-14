package com.nyami.bankstate;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@PluginDescriptor(
        name = "Expose Bank State",
        description = "Expose bank open/closed state to localhost for overlays",
        tags = {"bank","state","overlay","http"}
)
public class NyamiBankStatePlugin extends Plugin
{

    @Inject
    private Gson gson;
@Inject
    private Client client;

    @Inject
    private NyamiBankStateConfig config;

    private volatile boolean bankOpen = false;

    private HttpServer server;

    @Provides
    NyamiBankStateConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NyamiBankStateConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        refreshBankState();
        restartServer();
        log.info("Expose Bank State started. HTTP enabled={} port={}", config.enableHttp(), config.port());
    }

    @Override
    protected void shutDown() throws Exception
    {
        stopServer();
        log.info("Expose Bank State stopped.");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"exposebankstate".equals(e.getGroup()))
        {
            return;
        }

        // Any relevant config change: restart server
        restartServer();
        log.info("Expose Bank State config changed: {} = {}", e.getKey(), e.getNewValue());
    }

    // Lightweight polling approach: update bankOpen whenever we can
    // (This runs whenever Plugin is active; bank widget is the simplest reliable signal.)
    private void refreshBankState()
    {
        try
        {
            Widget bankContainer = client.getWidget(ComponentID.BANK_CONTAINER);
            bankOpen = bankContainer != null && !bankContainer.isHidden();
        }
        catch (Exception ex)
        {
            // Don't ever crash the client from this plugin.
            bankOpen = false;
        }
    }

    private synchronized void restartServer()
    {
        stopServer();

        if (!config.enableHttp())
        {
            return;
        }

        int port = config.port();
        if (port < 1024 || port > 65535)
        {
            log.warn("Port {} is invalid or privileged. Choose 1024-65535.", port);
            return;
        }

        try
        {
            // Bind localhost only
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

            server.createContext("/state", (HttpExchange ex) ->
            {
                try
                {
                    refreshBankState();
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("bankOpen", bankOpen);

                    byte[] body = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    ex.getResponseHeaders().set("Cache-Control", "no-store");
                    ex.sendResponseHeaders(200, body.length);

                    try (OutputStream os = ex.getResponseBody())
                    {
                        os.write(body);
                    }
                }
                catch (Exception err)
                {
                    byte[] body = ("{\"error\":\"" + err.getClass().getSimpleName() + "\"}").getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    ex.sendResponseHeaders(500, body.length);
                    try (OutputStream os = ex.getResponseBody())
                    {
                        os.write(body);
                    }
                }
                finally
                {
                    ex.close();
                }
            });

            server.setExecutor(Executors.newSingleThreadExecutor(r ->
            {
                Thread t = new Thread(r, "ExposeBankStateHttp");
                t.setDaemon(true);
                return t;
            }));

            server.start();
            log.info("Expose Bank State listening on http://127.0.0.1:{}/state", port);
        }
        catch (IOException ioe)
        {
            log.error("Failed to start HTTP server (port {}): {}", port, ioe.toString());
            stopServer();
        }
    }

    private synchronized void stopServer()
    {
        if (server != null)
        {
            try
            {
                server.stop(0);
            }
            catch (Exception ignored)
            {
            }
            server = null;
        }
    }
}
