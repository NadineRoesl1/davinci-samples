import com.vector.cfg.automation.model.ecuc.microsar.os.Os
import com.vector.cfg.automation.scripting.api.IScriptCreationApi
import com.vector.cfg.automation.scripting.api.IScriptFactory
import com.vector.cfg.automation.scripting.api.IScriptTaskTypeApi.DV_PROJECT
import com.vector.cfg.bswmdmodel.pai.api.bswmdModel
import com.vector.cfg.model.pai.api.transaction
import com.vector.cfg.sysdesc.model.component.SIAtomicComponent
import com.vector.cfg.sysdesc.model.pai.sysDescModel
import com.vector.ecusdk.appint.os.OsAppMapping
import com.vector.ecusdk.appint.os.mapSwcsToOsApps

class PartitionMapping : IScriptFactory {
    override fun createScript(creationApi: IScriptCreationApi) {
        creationApi.scriptTask("partitionMapping", DV_PROJECT) {
            taskDescription("Map software component instances to OS applications.")
            code {
                val logger = LoggerAdapter(scriptLogger)

                //TODO replace with the OS applications and software component instances of your project
                val osApp = project.bswmdModel(Os::class.java).single()
                    .osApplication.byName("SystemApplication_OsCore0").mdfObject
                val swc = sysDescModel.flatComponentView.filterIsInstance<SIAtomicComponent>()
                    .single { it.mdfObject.name == "CpApLidarStateMachine" }

                transaction {
                    mapSwcsToOsApps(project, logger, listOf(
                        OsAppMapping(component = swc, osApp = osApp)
                    ))
                }
            }
        }
    }
}
