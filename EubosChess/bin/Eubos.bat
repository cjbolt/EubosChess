@echo off
REM This batch file is to start Eubos as a process from e.g. Arena, use the UCI protocol via stdin/stdout to communicate with Eubos
REM java -XX:+UnlockDiagnosticVMOptions -Xshare:off -XX:+LogCompilation -XX:+TraceClassLoading -XX:MaxInlineSize=40 -Djdk.attach.allowAttachSelf -Xmx4G -Xms4G -jar .\Eubos.jar
java -Xshare:off -XX:MaxInlineSize=40 -Djdk.attach.allowAttachSelf -Xmx4G -Xms4G -jar .\Eubos.jar