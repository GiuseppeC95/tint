package eu.fbk.dh.tint.readability;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.pipeline.Annotator;
import eu.fbk.utils.gson.JSONLabel;

/**
 * Created by giovannimoretti on 19/05/16.
 */
public class ReadabilityAnnotations {

    public static Annotator.Requirement READABILITY_REQUIREMENT = new Annotator.Requirement("readability");

    @JSONLabel("readability")
    public static class ReadabilityAnnotation implements CoreAnnotation<Readability> {

        public Class<Readability> getType() {
            return Readability.class;
        }
    }

    @JSONLabel("hyphenation")
    public static class HyphenationAnnotation implements CoreAnnotation<String> {

        public Class<String> getType() {
            return String.class;
        }
    }

    @JSONLabel("difficultyLevel")
    public static class DifficultyLevelAnnotation implements CoreAnnotation<Integer> {

        public Class<Integer> getType() {
            return Integer.class;
        }
    }

}
