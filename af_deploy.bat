del /Q war
xcopy /Y /S "C:\Users\Owner\workspace\Servers\Tomcat v7.0 Server at localhost-config\web.xml" WebContent\WEB-INF\
xcopy /Y /S .\WebContent\WEB-INF\*.* war\WEB-INF
xcopy /Y /S .\build\classes\*.* war\WEB-INF\classes\
cd war
jar cvf ..\deploy\securemarytts.war * 
cd ..
cd deploy
call af login
call af update SecureMaryTTS
cd ..