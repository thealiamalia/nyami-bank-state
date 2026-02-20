package com.thealiamalia.exposebankstate;





import net.runelite.client.RuneLite;


import net.runelite.client.externalplugins.ExternalPluginManager;





public class ExposeBankStatePluginTest


{


	public static void main(String[] args) throws Exception


	{


		ExternalPluginManager.loadBuiltin(ExposeBankStatePlugin.class);


		RuneLite.main(args);


	}


}
