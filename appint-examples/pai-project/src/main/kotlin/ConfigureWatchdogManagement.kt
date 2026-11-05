import com.vector.cfg.automation.scripting.api.IScriptCreationApi
import com.vector.cfg.automation.scripting.api.IScriptFactory
import com.vector.cfg.automation.scripting.api.IScriptTaskTypeApi.DV_PROJECT
import com.vector.cfg.model.mdf.ar4x.swcomponenttemplate.components.MIApplicationSwComponentType
import com.vector.cfg.model.pai.api.AUTOSAR
import com.vector.cfg.model.pai.api.transaction
import com.vector.ecusdk.appint.base.assignInstructions
import com.vector.ecusdk.appint.wdgm.WatchdogConfig
import com.vector.ecusdk.appint.wdgm.WatchdogConfig.AliveSupervision
import com.vector.ecusdk.appint.wdgm.WatchdogConfig.CheckpointGraph
import com.vector.ecusdk.appint.wdgm.WatchdogConfig.ProgramFlowSupervision
import com.vector.ecusdk.appint.wdgm.WatchdogConfig.Transition
import com.vector.ecusdk.appint.wdgm.WatchdogConfig.WdgMInstruction
import com.vector.ecusdk.appint.wdgm.collectSupervisedEntityAppPatternInstances
import com.vector.ecusdk.appint.wdgm.configureWatchdogManagement
import com.vector.cfg.model.mdf.model.autosar.base.MIARPackage
import com.vector.cfg.model.mdf.model.autosar.base.MIAUTOSAR
import com.vector.cfg.model.mdf.model.autosar.base.MIPackageableElement
import kotlin.reflect.KClass
import kotlin.reflect.cast

class ConfigureWatchdogManagement : IScriptFactory {
    override fun createScript(creationApi: IScriptCreationApi) {
        creationApi.scriptTask("configureWatchdogManagement", DV_PROJECT) {
            taskDescription("Execute the Watchdog Management Top Down Service Configuration.")
            code {
                val logger = LoggerAdapter(scriptLogger)

                //TODO replace with instructions matching your needs
                val serviceDependencies = project.AUTOSAR.findElements<MIApplicationSwComponentType>()
                    .flatMap { it.internalBehavior.singleOrNull()?.serviceDependency ?: emptyList() }
                val lidarControlSupervisionCheckpoint1 =
                    serviceDependencies.single { it.name == "LidarControlSupervisionCheckpoint1" }
                val lidarControlSupervisionCheckpoint2 =
                    serviceDependencies.single { it.name == "LidarControlSupervisionCheckpoint2" }

                val config = WatchdogConfig(
                    supervisedEntityAppPatternInstances = collectSupervisedEntityAppPatternInstances(project).assignInstructions(
                        { "${it.contextSwc.name}.${it.serviceDependency.name}" },
                        "CpApLidarControl.LidarControlSupervision" to WdgMInstruction(
                            aliveSupervision = AliveSupervision(
                                supervisedCheckpoint = lidarControlSupervisionCheckpoint1,
                                expectedIndications = 1,
                                referenceCycle = 2,
                                minMargin = 1,
                                maxMargin = 2,
                            ),
                            programFlowSupervision = ProgramFlowSupervision(
                                referenceCycle = 2,
                                failedCycleTolerance = 0
                            ),
                            checkpointGraph = CheckpointGraph(
                                initial = lidarControlSupervisionCheckpoint1,
                                final = listOf(lidarControlSupervisionCheckpoint2),
                                transitions = listOf(
                                    Transition(
                                        src = lidarControlSupervisionCheckpoint1,
                                        dest = lidarControlSupervisionCheckpoint2
                                    )
                                )
                            )
                        )
                    )
                )

                transaction {
                    configureWatchdogManagement(project, logger, config)
                }
            }
        }
    }
}

internal inline fun <reified T : MIPackageableElement> MIAUTOSAR.findElements(): List<T> = findElements(T::class)

internal fun <T : MIPackageableElement> MIAUTOSAR.findElements(type: KClass<T>): List<T> {
    val result = mutableListOf<T>()
    findRecursive(subPackage, type, result)
    return result
}

private fun <T : MIPackageableElement> findRecursive(
    packs: List<MIARPackage>,
    type: KClass<T>,
    result: MutableList<T>
) {
    for (pack in packs) {
        for (element in pack.element) {
            if (type.isInstance(element)) {
                result += type.cast(element)
            }
        }
        findRecursive(pack.subPackage, type, result)
    }
}
