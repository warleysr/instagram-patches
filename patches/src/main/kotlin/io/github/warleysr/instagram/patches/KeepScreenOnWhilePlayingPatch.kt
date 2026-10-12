package io.github.warleysr.instagram.patches

import io.github.warleysr.instagram.patches.Constants.COMPATIBILITY_INSTAGRAM
import io.github.warleysr.instagram.patches.Constants.EXTENSION_PATH
import io.github.warleysr.instagram.patches.Constants.KEEP_SCREEN_ON_EXTENSION_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val keepScreenOnWhilePlayingPatch =
    bytecodePatch(
        name = "Keep screen on while playing video",
        description = "Keeps the screen on while a video is playing in the feed, stories and other places, like in Reels.",
    ) {
        compatibleWith(COMPATIBILITY_INSTAGRAM)
        extendWith(EXTENSION_PATH)

        execute {

            val setKeepScreenOnMethod = playerSetKeepScreenOnMethod()

            VideoStartPlayingFingerprint.method.apply {
                val settingIndex = VideoStartPlayingFingerprint.instructionMatches.last().index
                val register = getInstruction<TwoRegisterInstruction>(settingIndex).registerA

                addInstructions(
                    settingIndex + 1,
                    """
                    invoke-static {v$register}, $KEEP_SCREEN_ON_EXTENSION_CLASS->keepScreenOnWhilePlaying(Z)Z
                    move-result v$register
                    """.trimIndent(),
                )
            }

            val playerClass = setKeepScreenOnMethod.parameterTypes[0].toString()

            // Completion callback of the player controller, which reads the looping flag of the player:
            //
            //   iget-object v0, v2, Controller;->player:Player;
            //   iget-object v0, v0, Player;->config:Config;
            //   iget-boolean v0, v0, Config;->isLooping:Z
            //   const/16 v1, 0x3a
            val loopingFingerprint =
                Fingerprint(
                    returnType = "V",
                    parameters = listOf("J"),
                    filters =
                        listOf(
                            fieldAccess(type = playerClass, opcode = Opcode.IGET_OBJECT),
                            fieldAccess(
                                definingClass = playerClass,
                                opcode = Opcode.IGET_OBJECT,
                                location = InstructionLocation.MatchAfterImmediately(),
                            ),
                            fieldAccess(
                                type = "Z",
                                opcode = Opcode.IGET_BOOLEAN,
                                location = InstructionLocation.MatchAfterImmediately(),
                            ),
                            literal(0x3a, location = InstructionLocation.MatchAfterImmediately()),
                        ),
                )
            val (configField, loopingField) =
                loopingFingerprint.instructionMatches.let { matches ->
                    matches[1].instruction.getReference<FieldReference>()!! to
                        matches[2].instruction.getReference<FieldReference>()!!
                }

            VideoCompletionFingerprint.method.apply {
                val (stopIndex, stopCall) = videoCompletionStopCall(playerClass)
                val playerRegister = stopCall.registerC
                val freeRegister = stopCall.registerD

                addInstructions(
                    stopIndex + 1,
                    """
                    iget-object v$freeRegister, v$playerRegister, $configField
                    iget-boolean v$freeRegister, v$freeRegister, $loopingField
                    invoke-static {v$freeRegister}, $KEEP_SCREEN_ON_EXTENSION_CLASS->keepScreenOnWhileLooping(Z)Z
                    move-result v$freeRegister
                    invoke-static {v$playerRegister, v$freeRegister}, $setKeepScreenOnMethod
                    """.trimIndent(),
                )
            }
        }
    }
