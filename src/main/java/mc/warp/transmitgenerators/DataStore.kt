package mc.warp.transmitgenerators

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import mc.warp.transmitgenerators.gson.GeneratorSerializer
import mc.warp.transmitgenerators.gson.ItemStackSerializer
import mc.warp.transmitgenerators.gson.LocationSerializer
import mc.warp.transmitgenerators.type.WarpPlayer
import mc.warp.transmitgenerators.type.WarpSound
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap


class DataStore {

    var playerList = HashMap<UUID, WarpPlayer>()

    var sounds = HashMap<String, WarpSound>()
    lateinit var messages: YamlConfiguration;
    lateinit var config: YamlConfiguration;
    private var genList = HashMap<String, Generator>()
    private var langFile: File;
    private var soundFile: File;
    private var dataFolder: File;
    private var genFile: File;
    private var configFile: File;
    private var playerFolder: File;
    var GSON: Gson;

    constructor() {
        dataFolder = TransmitGenerators.getInstance().dataFolder
        genFile = File(dataFolder.absolutePath + "/gens.json")
        configFile = File(dataFolder.absolutePath + "/config.yml")
        langFile = File(dataFolder.absolutePath + "/lang.yml")
        soundFile = File(dataFolder.absolutePath + "/sounds.json")
        playerFolder = File(dataFolder.absolutePath + "/players")
        GSON = GsonBuilder()
            .registerTypeAdapter(Location::class.java, LocationSerializer())
            .registerTypeAdapter(ItemStack::class.java, ItemStackSerializer())
            .registerTypeAdapter(Generator::class.java, GeneratorSerializer())
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    }




    fun DataInitialization() {
        dataFolder.mkdir()

        if (!playerFolder.exists()) {
            playerFolder.mkdir()
        }
        if (!configFile.exists()) {
            configFile.createNewFile()
            copyResourceFile("config.yml", configFile)
        }
        loadConfig()
        var guiFolder = File(dataFolder.absolutePath + "/guis")
        if (!guiFolder.exists()) {
            guiFolder.mkdir()
            copyResourceFile("guis/genList.xml", File(guiFolder.absolutePath + "/genList.xml"))

        }
        if (!langFile.exists()) {
            langFile.createNewFile()
            copyResourceFile("lang.yml", langFile)
        }
        loadLang()
        if (!soundFile.exists()) {
            soundFile.createNewFile()
            copyResourceFile("sounds.json", soundFile)
        }
        loadSound()
        if (!genFile.exists()) {
            genFile.createNewFile()
            Generator("dirt", ItemStack(Material.DIRT), ItemStack(Material.BROWN_DYE), 1, "stone", 50);
            Generator("stone", ItemStack(Material.STONE), ItemStack(Material.STONE_BUTTON), 2, "coal", 100);
            saveGenerators()
        } else {
            loadGenerators()
        }
    }

    fun loadGenerators() {
        if (!genFile.exists()) throw NullPointerException("Generator Save File is null")

        var Reader = FileReader(genFile)
        var type = object : TypeToken<ArrayList<Generator>>() {} .type;
        var result = GSON.fromJson<ArrayList<Generator>>(Reader, type)
        for (gen in result) {
            this.setGenerator(gen.id, gen)
        }

    }

    fun saveGenerators() {

        if (!genFile.exists()) {
            genFile.createNewFile()
        }

        val writer: Writer = FileWriter(genFile, false)
        val genList = ArrayList<Generator>()
        for (gen in this.getAllGenerators()) {
            TransmitGenerators.getInstance().logger.info("Generator: (${gen.value.id}) has been saved")
            genList.add(gen.value);
        }

        GSON.toJson(genList, writer)
        writer.flush()
        writer.close()
    }

    fun loadPlayer(player: Player): WarpPlayer {
        var saveFile = File(playerFolder.absolutePath + "/${player.uniqueId}.json")


        if (!saveFile.exists()) {
            saveFile.createNewFile()

            return WarpPlayer(player)
        }

        var Reader = FileReader(saveFile)
        var type = object : TypeToken<WarpPlayer>() {} .type
        var result = GSON.fromJson<WarpPlayer>(Reader, type)
        return result
    }
    fun firstPlayerLoad(player: Player) {
        var playerData = TransmitGenerators.getDataStore().loadPlayer(player)
        TransmitGenerators.getDataStore().setPlayer(player, playerData);
        playerData.canUpgrade = true
    }

    fun savePlayer(player: Player) {
        var playerData: WarpPlayer = this.getPlayer(player) ?: return
        var saveFile = File(playerFolder.absolutePath + "/${player.uniqueId}.json")

        if (!saveFile.exists()) {
            saveFile.createNewFile()
        }
        var writer: Writer = FileWriter(saveFile, false)

        GSON.toJson(playerData, writer)
        writer.flush()
        writer.close()

    }


    fun copyResourceFile(resource: String, file: File) {
        var resourceFile = TransmitGenerators.getInstance().getResource(resource)
        var resourceString = BufferedReader(
            InputStreamReader(resourceFile, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"))
        val writer: Writer = FileWriter(file, false)
        writer.write(resourceString)
        writer.flush()
        writer.close()
    }


    fun loadLang() {
        if (!langFile.exists()) throw NullPointerException("Lang Save File is null")

        messages = YamlConfiguration.loadConfiguration(langFile)
    }
    fun loadConfig() {

        if (!configFile.exists()) throw NullPointerException("Config file does not exist")

        config = YamlConfiguration.loadConfiguration(configFile)

    }

    fun saveLang() {
        if (!langFile.exists()) {
            langFile.createNewFile()
        }

        val writer: Writer = FileWriter(langFile, false)
        GSON.toJson(messages, writer)
        writer.flush()
        writer.close()
    }


    fun loadSound() {
        if (!soundFile.exists()) throw NullPointerException("Sound Save File is null")

        var Reader = FileReader(soundFile)
        var type = object : TypeToken<HashMap<String, WarpSound>>() {} .type;
        var result = GSON.fromJson<HashMap<String, WarpSound>>(Reader, type)
        sounds = result
    }
    fun saveSound() {
        if (!soundFile.exists()) {
            soundFile.createNewFile()
        }

        val writer: Writer = FileWriter(soundFile, false)
        GSON.toJson(sounds, writer)
        writer.flush()
        writer.close()
    }


    fun getGenerator(gen: String): Generator? {
        return genList.get(gen)
    }

    fun getAllGenerators(): HashMap<String, Generator> {
        return genList
    }
    fun setAllGenerators(gens: HashMap<String, Generator>) {
        genList = gens
    }

    fun setGenerator(ID: String, generator: Generator ) {
        genList.put(ID, generator)
    }

    fun getPlayer(player: Player): WarpPlayer? {
        return playerList.get(player.uniqueId)
    }

    fun setPlayer(player: Player, WarpPlayer: WarpPlayer) {
        playerList.put(player.uniqueId, WarpPlayer)
    }





}