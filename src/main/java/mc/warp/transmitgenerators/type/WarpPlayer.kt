package mc.warp.transmitgenerators.type

import com.google.gson.annotations.SerializedName
import de.tr7zw.nbtapi.NBTBlock
import mc.warp.transmitgenerators.TransmitGenerators
import mc.warp.transmitgenerators.TransmitGenerators.Companion.getDataStore
import mc.warp.transmitgenerators.utils.scheduler.schedule
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class WarpPlayer {

    @SerializedName("UUID") var UUID: UUID;
    @SerializedName("MaxGens") var maxGenSlots: Int;
    @SerializedName("PlacedGens") var placedGenSlots: Int;
    @SerializedName("Gens") var placedGens: HashMap<String, ArrayList<Location>>
    @Transient var canUpgrade: Boolean = true;

    constructor(player: Player) {
        this.UUID = player.uniqueId
        this.maxGenSlots = getDataStore().config.getInt("default-genslots")
        this.placedGenSlots = 0
        this.placedGens = HashMap()

    }


    fun genDrop() {
        var player = this
        var genWait = 10
        Bukkit.getScheduler().schedule(TransmitGenerators.getInstance()) {

            for (type in player.placedGens) {
                var toDelete = ArrayList<Location>()
                for ( loc in type.value) {
                    if (!loc.isChunkLoaded) continue
                    if (genWait == 0) {
                        genWait = 10
                        waitFor(1)
                    }
                    var compound = NBTBlock(loc.block).data.getCompound("TransmitNBT") ?: continue
                    var genID = compound.getString("generator") ?: continue
                    var gen = getDataStore().getGenerator(genID) ?: continue
                    if (loc.block.type != gen.getBlock().type) {
                        NBTBlock(loc.block).data.removeKey("TransmitNBT")
                        toDelete.add(loc)

                    }
                    var newloc = loc.clone().add(0.5,1.0,0.5)
                    loc.world.dropItem(newloc, gen.getDrop()).velocity = Vector(0.0,0.1,0.0)
                    genWait -= TransmitGenerators.genWait
                }
                type.value.removeAll(toDelete.toSet());
            }



        }

    }

}