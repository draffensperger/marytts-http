marytts-http
===========

HTTP server front end to [MaryTTS](http://github.com/marytts/marytts) (text-to-speech engine).

Usage
-------

The server sends wave formatted audio for the given text and voice inputs.

The following main query string parameters can be specified:
- `text`: The text to turn into speech
- `locale` (optional): The voice locale (e.g. "de" for German). It defaults to 
  English ("en").
- `gender` (optional): Allows you to select the voice for the locale based on
  its gender (i.e. 'male' or 'female').
- `voice` (optional): If there are multiple MaryTTS voices installed for a
  language, you can choose the specific one with this.
- `style` (optional): Allows you to specify a style for voices that support
  multiple styles.
- `effects` (optional): Allows you to specify effects for the voice.

To better understand these options and how they are used by MaryTTS, you can
[install MaryTTS locally](http://mary.dfki.de/download/) and experiment with it.
You can also try the MaryTTS [online demo](http://mary.dfki.de:59125/), though
as of this writing the demo is sometimes down.

To play the audio on a web page you can embed it with the `<audio>` tag, e.g.:
```
<audio autoplay>
<source src="maryserver.example.com/?text=Hello+World" type="audio/wav">
</audio>
```

Securing requests
-------

If you wish to secure your requests so that a third party couldn't use your
text-to-speech server for audio clips not related to your application, you can
secure `marytts-server` by setting the `HMAC_SECRET` environment variable, which 
will then require requests to be signed with that secret using HMAC-SHA256.

You can also specify the `expires` (Unix timestamp) parameter with your requests
so that they can only be played for a certain amount of time.

The string you need to sign is the concatenation of the various options (with
    blanks for the ones you didn't specify), i.e.
`text + locale + gender + voice + style + effects + expires`.

The HMAC-SHA256 result should be sent in the `signature` parameter. 
So, for example, if your `HMAC_SECRET` were 
`8Z2X4ZZyI0+2Ud35CaPk4bSe+rjjFiIQkMWjBYj2Q5M=` (it's stored in base64), then a
signed URL would look like this:
``````
https://marytts-http.example.com/?text=der+Fuchs&locale=de&expires=1437441339&signature=nrZ5adPD6pAAHiTS5Mh7KgWD2I9QyPYAQ01p2U8eLr4%3D
```

Here's sample PHP code for signing a request and embedding it in a page:



Deployment
---------

MaryTTS server uses Jetty and it can easily be deployed to Heroku. Just clone
this repository and then push it to a Heroku remote. That will trigger the Maven
build which will fetch all of the necessary dependencies.

To build it locally with Maven run `mvn install` and then to execute the server 
with the following command:
```
java -cp target/classes:target/dependency/*:target/voices/lib/* maryserver.MaryServer
```

Potential enhancements
-----------

If you want to add more voices or languages, you can modify `pom.xml` to pull in 
more language jars and download more voices. 

A list of voices is shown in the 
[marytts-components.xml](https://github.com/marytts/marytts/blob/master/download/marytts-components.xml)
file in the marytts project.

Another idea would be to support ogg/vorbis encoding using 
[VorbisJava](https://github.com/Gagravarr/VorbisJava).

Utilizing caching (perhaps via Amazon S3 to store generated clips) could also be 
useful.

License
-------

This code itself itself is licensed under the simplified BSD License (see License.md)

MaryTTS is licensed under the 
