/*
 * Copyright (C) 2017 Christopher J. Stehno <chris@stehno.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stehno.gradle.natives

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ListNativesTaskSpec extends Specification {

    // TODO: test with different gradle versions: .withGradleVersion()

    @Rule public TemporaryFolder projectDir = new TemporaryFolder()

    def 'listNatives with no dependencies should succeed'() {
        given: 'an build file with no native configuration and no native dependencies'
        buildFile()

        when: 'the task is run'
        BuildResult result = gradleRunner(['listNatives']).build()

        then: 'the build should pass'
        println result.output
        totalSuccess result
    }

    def 'listNatives with native dependencies should list them (default config)'() {
        given:
        buildFile([
            dependencies: /compile 'org.lwjgl.lwjgl:lwjgl:2.9.1'/
        ])

        when: 'the task is run'
        BuildResult result = gradleRunner(['listNatives']).build()

        then:
        textContainsLines result.output, [
            '- jinput-platform-2.0.5-natives-linux.jar:',
            '[LINUX] libjinput-linux.so',
            '[LINUX] libjinput-linux64.so',
            '- lwjgl-platform-2.9.1-natives-linux.jar:',
            '[LINUX] libopenal64.so',
            '[LINUX] liblwjgl.so',
            '[LINUX] liblwjgl64.so',
            '[LINUX] libopenal.so'
        ]
    }

    def 'listNatives with native dependencies should list them (windows)'() {
        given:
        buildFile([
            dependencies: /compile 'org.lwjgl.lwjgl:lwjgl:2.9.1'/,
            natives     : '''
                natives {
                    platforms = [Platform.WINDOWS]
                }
            '''.stripIndent()
        ])

        when: 'the task is run'
        BuildResult result = gradleRunner(['listNatives']).build()

        then:
        textContainsLines result.output, [
            '- lwjgl-platform-2.9.1-natives-windows.jar:',
            '[WINDOWS] lwjgl.dll',
            '[WINDOWS] OpenAL64.dll',
            '[WINDOWS] OpenAL32.dll',
            '[WINDOWS] lwjgl64.dll',
            '- jinput-platform-2.0.5-natives-windows.jar:',
            '[WINDOWS] jinput-dx8_64.dll',
            '[WINDOWS] jinput-dx8.dll',
            '[WINDOWS] jinput-wintab.dll',
            '[WINDOWS] jinput-raw_64.dll',
            '[WINDOWS] jinput-raw.dll'
        ]
    }

    def 'listNatives with native dependencies should list them (windows,linux)'() {
        given:
        buildFile([
            dependencies: /compile 'org.lwjgl.lwjgl:lwjgl:2.9.1'/,
            natives     : '''
                natives {
                    platforms = [Platform.WINDOWS, Platform.LINUX]
                }
            '''.stripIndent()
        ])

        when: 'the task is run'
        BuildResult result = gradleRunner(['listNatives']).build()

        then:
        textContainsLines result.output, [
            '- lwjgl-platform-2.9.1-natives-windows.jar:',
            '[WINDOWS] lwjgl.dll',
            '[WINDOWS] OpenAL64.dll',
            '[WINDOWS] OpenAL32.dll',
            '[WINDOWS] lwjgl64.dll',
            '- jinput-platform-2.0.5-natives-linux.jar:',
            '[LINUX] libjinput-linux.so',
            '[LINUX] libjinput-linux64.so',
            '- lwjgl-platform-2.9.1-natives-linux.jar:',
            '[LINUX] libopenal64.so',
            '[LINUX] liblwjgl.so',
            '[LINUX] liblwjgl64.so',
            '[LINUX] libopenal.so',
            '- jinput-platform-2.0.5-natives-windows.jar:',
            '[WINDOWS] jinput-dx8_64.dll',
            '[WINDOWS] jinput-dx8.dll',
            '[WINDOWS] jinput-wintab.dll',
            '[WINDOWS] jinput-raw_64.dll',
            '[WINDOWS] jinput-raw.dll'
        ]
    }

    def 'listNatives for some of the trouble libs on all platforms'() {
        given:
        buildFile([
            dependencies: '''
                compile 'org.lwjgl:lwjgl:3.0.0'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-windows'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-linux'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-osx'
            '''.stripIndent(),
            natives     : '''
                natives {
                    platforms = Platform.all()
                }
            '''.stripIndent()
        ])

        when: 'the task is run'
        BuildResult result = gradleRunner(['listNatives']).build()

        then:
        textContainsLines result.output, [
            '- lwjgl-platform-3.0.0-natives-windows.jar:',
            '[WINDOWS] lwjgl.dll',
            '[WINDOWS] lwjgl32.dll',
            '[WINDOWS] OpenAL.dll',
            '[WINDOWS] jemalloc.dll',
            '[WINDOWS] glfw.dll',
            '[WINDOWS] glfw32.dll',
            '[WINDOWS] jemalloc32.dll',
            '[WINDOWS] OpenAL32.dll',
            '- lwjgl-platform-3.0.0-natives-osx.jar:',
            '[MAC] liblwjgl.dylib',
            '[MAC] libjemalloc.dylib',
            '[MAC] libglfw.dylib',
            '[MAC] libopenal.dylib',
            '- lwjgl-platform-3.0.0-natives-linux.jar:',
            '[LINUX] libjemalloc.so',
            '[LINUX] liblwjgl.so',
            '[LINUX] libglfw.so',
            '[LINUX] libopenal.so'
        ]
    }

    def 'listNatives for some of the trouble libs on all platforms (lib excludes)'() {
        given:
        buildFile([
            dependencies: '''
                compile 'org.lwjgl:lwjgl:3.0.0'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-windows'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-linux'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-osx'
            '''.stripIndent(),
            natives     : '''
                natives {
                    platforms = Platform.all()
                    libraries {
                        exclude = ['lwjgl32.dll', 'libjemalloc.dylib']
                    }
                }
            '''.stripIndent()
        ])

        when: 'the task is run'
        BuildResult result = gradleRunner(['listNatives']).build()

        then:
        textContainsLines result.output, [
            '- lwjgl-platform-3.0.0-natives-windows.jar:',
            '[WINDOWS] lwjgl.dll',
            '[WINDOWS] OpenAL.dll',
            '[WINDOWS] jemalloc.dll',
            '[WINDOWS] glfw.dll',
            '[WINDOWS] glfw32.dll',
            '[WINDOWS] jemalloc32.dll',
            '[WINDOWS] OpenAL32.dll',
            '- lwjgl-platform-3.0.0-natives-osx.jar:',
            '[MAC] liblwjgl.dylib',
            '[MAC] libglfw.dylib',
            '[MAC] libopenal.dylib',
            '- lwjgl-platform-3.0.0-natives-linux.jar:',
            '[LINUX] libjemalloc.so',
            '[LINUX] liblwjgl.so',
            '[LINUX] libglfw.so',
            '[LINUX] libopenal.so'
        ]
        textDoesNotContainLines result.output, ['[WINDOWS] lwjgl32.dll', '[MAC] libjemalloc.dylib',]
    }

    def 'listNatives for some of the trouble libs on all platforms (includes filter)'() {
        given:
        buildFile([
            dependencies: '''
                compile 'org.lwjgl:lwjgl:3.0.0'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-windows'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-linux'
                compile 'org.lwjgl:lwjgl-platform:3.0.0:natives-osx'
            '''.stripIndent(),
            natives     : '''
                natives {
                    platforms = Platform.all()
                    libraries {
                        include = ['OpenAL.dll', 'libopenal.dylib', 'libopenal.so']
                    }
                }
            '''.stripIndent()
        ])

        when: 'the task is run'
        BuildResult result = gradleRunner(['listNatives']).build()

        then:
        textContainsLines result.output, [
            '- lwjgl-platform-3.0.0-natives-windows.jar:',
            '[WINDOWS] OpenAL.dll',
            '- lwjgl-platform-3.0.0-natives-osx.jar:',
            '[MAC] libopenal.dylib',
            '- lwjgl-platform-3.0.0-natives-linux.jar:',
            '[LINUX] libopenal.so'
        ]
        textDoesNotContainLines result.output, [
            '[WINDOWS] lwjgl.dll',
            '[WINDOWS] lwjgl32.dll',
            '[WINDOWS] jemalloc.dll',
            '[WINDOWS] glfw.dll',
            '[WINDOWS] glfw32.dll',
            '[WINDOWS] jemalloc32.dll',
            '[WINDOWS] OpenAL32.dll',
            '[MAC] liblwjgl.dylib',
            '[MAC] libjemalloc.dylib',
            '[MAC] libglfw.dylib',
            '[LINUX] libjemalloc.so',
            '[LINUX] liblwjgl.so',
            '[LINUX] libglfw.so',
        ]
    }

    private void buildFile(final Map<String, Object> config = [:]) {
        File buildFile = projectDir.newFile('build.gradle')
        buildFile.text = """
            import com.stehno.gradle.natives.ext.Platform

            plugins {
                id 'com.stehno.natives'
                id 'java'
            }
            repositories {
                jcenter()
            }
            dependencies {
                ${config.dependencies ?: ''}
            }
            ${config.natives ?: ''}
        """.stripIndent()
    }

    private GradleRunner gradleRunner(final List<String> args) {
        GradleRunner.create().withPluginClasspath().withDebug(true).withProjectDir(projectDir.root).withArguments(args)
    }

    private static boolean totalSuccess(final BuildResult result) {
        result.tasks.every { BuildTask task -> task.outcome != TaskOutcome.FAILED }
    }

    private static boolean textContainsLines(final String text, final Collection<String> lines, final boolean trimmed = true) {
        lines.every { String line ->
            text.contains(trimmed ? line.trim() : line)
        }
    }

    private static boolean textDoesNotContainLines(final String text, final Collection<String> lines, final boolean trimmed = true) {
        lines.every { String line ->
            !text.contains(trimmed ? line.trim() : line)
        }
    }
}
