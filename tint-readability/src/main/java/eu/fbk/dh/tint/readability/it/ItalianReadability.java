package eu.fbk.dh.tint.readability.it;

import com.google.common.collect.HashMultimap;
import com.itextpdf.layout.hyphenation.Hyphenator;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dh.tint.readability.DescriptionForm;
import eu.fbk.dh.tint.readability.GlossarioEntry;
import eu.fbk.dh.tint.readability.Readability;
import eu.fbk.dh.tint.readability.ReadabilityAnnotations;
import eu.fbk.utils.gson.JSONExclude;

import java.util.*;

/**
 * Created by alessio on 21/09/16.
 */

abstract class ItalianReadability extends Readability {

    @JSONExclude ItalianReadabilityModel model;
    @JSONExclude int level1WordSize = 0, level2WordSize = 0, level3WordSize = 0;

    @JSONExclude StringBuilder buffer = new StringBuilder();
    @JSONExclude int lemmaIndex = 0;
    @JSONExclude HashMap<Integer, Integer> lemmaIndexes = new HashMap<>();
    @JSONExclude HashMap<Integer, Integer> tokenIndexes = new HashMap<>();
    TreeMap<Integer, DescriptionForm> forms = new TreeMap<>();

    @Override public void finalizeReadability() {
        double gulpease = 89 + (300 * getSentenceCount() - 10 * getDocLenLettersOnly()) / (getWordCount() * 1.0);
        measures.put("gulpease", gulpease);
        measures.put("level1", 100.0 * level1WordSize / getContentEasyWordSize());
        measures.put("level2", 100.0 * level2WordSize / getContentWordSize());
        measures.put("level3", 100.0 * level3WordSize / getContentWordSize());

        String lemmaText = buffer.toString().trim();
        String text = annotation.get(CoreAnnotations.TextAnnotation.class);

        HashMap<String, GlossarioEntry> glossario = model.getGlossario();

        List<String> glossarioKeys = new ArrayList<>(glossario.keySet());
        Collections.sort(glossarioKeys, new StringLenComparator());

        for (String form : glossarioKeys) {

            int numberOfTokens = form.split("\\s+").length;
            List<Integer> allOccurrences = findAllOccurrences(text, form);
            List<Integer> allLemmaOccurrences = findAllOccurrences(lemmaText, form);

            for (Integer occurrence : allOccurrences) {
                addDescriptionForm(form, tokenIndexes, occurrence, numberOfTokens, forms, annotation, glossario);
            }
            for (Integer occurrence : allLemmaOccurrences) {
                addDescriptionForm(form, lemmaIndexes, occurrence, numberOfTokens, forms, annotation, glossario);
            }
        }

    }

    public ItalianReadability(Properties globalProperties, Properties localProperties, Annotation annotation) {
        super("it", annotation);
        hyphenator = new Hyphenator("it", "it", 1, 1);
        model = ItalianReadabilityModel.getInstance(globalProperties, localProperties);
    }

    static class StringLenComparator implements Comparator<String> {

        public int compare(String s1, String s2) {
            return s1.length() - s2.length();
        }
    }

    public static List<Integer> findAllOccurrences(String haystack, String needle) {

        List<Integer> ret = new ArrayList<>();

        int index = haystack.indexOf(needle);
        while (index >= 0) {
            try {
                String afterChar = haystack.substring(index + needle.length(), index + needle.length() + 1);
                if (!afterChar.matches("\\w+")) {
                    ret.add(index);
                }
            } catch (Exception e) {
                // ignore
            }
            index = haystack.indexOf(needle, index + 1);
        }

        return ret;
    }

    static private void addDescriptionForm(String form, HashMap<Integer, Integer> indexes, int start,
            int numberOfTokens, TreeMap<Integer, DescriptionForm> forms, Annotation annotation,
            HashMap<String, GlossarioEntry> glossario) {
        Integer lemmaIndex = indexes.get(start);
        if (lemmaIndex == null) {
            return;
        }

        CoreLabel firstToken = annotation.get(CoreAnnotations.TokensAnnotation.class).get(lemmaIndex);
        CoreLabel endToken = annotation.get(CoreAnnotations.TokensAnnotation.class)
                .get(lemmaIndex + numberOfTokens - 1);
        Integer beginOffset = firstToken.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
        Integer endOffset = endToken.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);

        GlossarioEntry glossarioEntry = glossario.get(form);
        if (glossarioEntry == null) {
            return;
        }

        DescriptionForm descriptionForm = new DescriptionForm(
                beginOffset, endOffset, glossarioEntry);

        forms.put(beginOffset, descriptionForm);
    }

    @Override public void addingContentWord(CoreLabel token) {
        HashMap<Integer, HashMultimap<String, String>> easyWords = model.getEasyWords();
        String simplePos = getGenericPos(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
        String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);

        if (easyWords.get(3).get(simplePos).contains(lemma)) {
            level3WordSize++;
            token.set(ReadabilityAnnotations.DifficultyLevelAnnotation.class, 3);
        }
        if (easyWords.get(2).get(simplePos).contains(lemma)) {
            level2WordSize++;
            token.set(ReadabilityAnnotations.DifficultyLevelAnnotation.class, 2);
        }
        if (easyWords.get(1).get(simplePos).contains(lemma)) {
            level1WordSize++;
            token.set(ReadabilityAnnotations.DifficultyLevelAnnotation.class, 1);
        }
    }

    @Override public void addingEasyWord(CoreLabel token) {

    }

    @Override public void addingWord(CoreLabel token) {

    }

    @Override public void addingToken(CoreLabel token) {
        lemmaIndexes.put(buffer.length(), lemmaIndex);
        tokenIndexes.put(token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class), lemmaIndex);
        lemmaIndex++;
        buffer.append(token.get(CoreAnnotations.LemmaAnnotation.class)).append(" ");

    }

    @Override public void addingSentence(CoreMap sentence) {

    }
}
