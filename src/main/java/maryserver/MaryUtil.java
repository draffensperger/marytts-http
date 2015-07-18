package maryserver;

import java.util.Locale;
import java.io.OutputStream;
import marytts.server.Mary;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.Voice.Gender;

public class MaryUtil {
  public static void writeWaveAudio(OutputStream out, String text, String locale,
      String gender, String voiceName, String style, String effects) throws Exception
  {
    if (voiceName == "") {
      Voice voice = defaultVoice(locale, gender);
      voiceName = voice.getName();
      locale = voice.getLocale().toString();
    }

    Mary.process(text, "TEXT", "AUDIO", locale, "WAVE", voiceName, style,
        effects, "", out);

    out.flush();
  }

  private static Voice defaultVoice(String localeString, String gender) {
    Voice voice;
    Locale locale = localeFromString(localeString);
    if (gender != "") {
      voice = Voice.getVoice(locale, new Gender(gender));
    } else {
      voice = Voice.getDefaultVoice(locale);
    }

    if (voice == null) {
      voice = defaultVoiceFuzzyLocale(locale);
    }
    if (voice == null) {
      throw new IllegalArgumentException("No voice for " + locale + ":" + gender);
    }
    return voice;
  }

  private static Locale localeFromString(String localeString) {
    int underscoreLoc = localeString.indexOf('_');
    if (underscoreLoc == -1) {
      return new Locale(localeString);
    } else {
      String language = localeString.substring(0, underscoreLoc);
      String country = localeString.substring(underscoreLoc);
      return new Locale(language, country);
    }
  }

  private static Voice defaultVoiceFuzzyLocale(Locale locale) {
    locale = new Locale(locale.getLanguage());
    Voice voice = Voice.getDefaultVoice(locale);
    if (voice != null) {
      return voice;
    }

    if (locale.getLanguage() == "en") {
      locale = new Locale("en", "US");
    }
    return Voice.getDefaultVoice(locale);
  }
}
