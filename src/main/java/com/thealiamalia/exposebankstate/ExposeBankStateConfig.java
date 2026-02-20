package com.thealiamalia.exposebankstate;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("exposebankstate")
public interface ExposeBankStateConfig extends Config {
  @ConfigItem(
      keyName = "enableHttp",
      name = "Enable HTTP server",
      description = "Expose bank open/closed state over HTTP on localhost")
  default boolean enableHttp() {
    return true;
  }

  @ConfigItem(
      keyName = "port",
      name = "HTTP port",
      description = "Localhost port to bind (127.0.0.1). Example: 8337")
  default int port() {
    return 8337;
  }
}
