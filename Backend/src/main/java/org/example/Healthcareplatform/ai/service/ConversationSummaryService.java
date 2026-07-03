package org.example.Healthcareplatform.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ConversationSummaryService {

    private static final List<Extractor> EXTRACTORS = List.of(
            new SymptomExtractor(),
            new RecommendationExtractor(),
            new MedicationExtractor(),
            new AllergyExtractor(),
            new ConditionExtractor()
    );

    public String summarize(List<ConversationMessage> olderMessages) {
        if (olderMessages == null || olderMessages.isEmpty()) {
            return "";
        }

        var summary = new StringBuilder();
        summary.append("--- Conversation Summary ---\n");

        for (Extractor extractor : EXTRACTORS) {
            var findings = extractor.extract(olderMessages);
            if (!findings.isEmpty()) {
                summary.append(extractor.label()).append(": ");
                summary.append(String.join(", ", findings));
                summary.append(".\n");
            }
        }

        summary.append("--- End Summary ---");

        String result = summary.toString();
        log.debug("Generated conversation summary ({} chars) from {} messages",
                result.length(), olderMessages.size());
        return result;
    }

    interface Extractor {
        String label();
        Set<String> extract(List<ConversationMessage> messages);
    }

    record SymptomExtractor() implements Extractor {
        private static final Pattern SYMPTOM_PATTERN = Pattern.compile(
                "(?i)\\b(" +
                        "headache|migraine|fever|cough|sore throat|runny nose|nausea|vomiting" +
                        "|diarrhea|constipation|fatigue|dizziness|rash|itching|swelling" +
                        "|chest pain|shortness of breath|back pain|joint pain|muscle pain" +
                        "|insomnia|anxiety|depression|blurred vision|ringing in ears" +
                        "|numbness|tingling|weight loss|weight gain|loss of appetite" +
                        "|frequent urination|blood in urine|blood in stool" +
                        ")\\b");

        @Override
        public String label() { return "Symptoms mentioned"; }

        @Override
        public Set<String> extract(List<ConversationMessage> messages) {
            var found = new HashSet<String>();
            for (ConversationMessage msg : messages) {
                Matcher m = SYMPTOM_PATTERN.matcher(msg.getContent());
                while (m.find()) {
                    found.add(m.group(1).toLowerCase());
                }
            }
            return found;
        }
    }

    record RecommendationExtractor() implements Extractor {
        private static final Pattern RECO_PATTERN = Pattern.compile(
                "(?i)\\b(" +
                        "consult a doctor|see a doctor|visit a clinic|go to hospital" +
                        "|drink water|hydration|rest|sleep|exercise|diet|balanced meal" +
                        "|avoid caffeine|avoid alcohol|quit smoking|reduce stress" +
                        "|apply ice|apply heat|warm compress|cold compress" +
                        "|elevate|stretch|physical therapy|follow up" +
                        "|take medication|over-the-counter|prescription" +
                        ")\\b");

        @Override
        public String label() { return "Previously recommended"; }

        @Override
        public Set<String> extract(List<ConversationMessage> messages) {
            var found = new HashSet<String>();
            for (ConversationMessage msg : messages) {
                if (msg.getRole() != ConversationMessage.MessageRole.ASSISTANT) continue;
                Matcher m = RECO_PATTERN.matcher(msg.getContent());
                while (m.find()) {
                    found.add(m.group(1).toLowerCase());
                }
            }
            return found;
        }
    }

    record MedicationExtractor() implements Extractor {
        private static final Pattern MED_PATTERN = Pattern.compile(
                "(?i)\\b(" +
                        "paracetamol|acetaminophen|ibuprofen|aspirin|naproxen" +
                        "|amoxicillin|penicillin|metformin|insulin|atorvastatin" +
                        "|lisinopril|amlodipine|metoprolol|omeprazole" +
                        "|antihistamine|cetirizine|loratadine|diphenhydramine" +
                        "|vitamin C|vitamin D|zinc|iron|calcium|magnesium" +
                        ")\\b");

        @Override
        public String label() { return "Medications discussed"; }

        @Override
        public Set<String> extract(List<ConversationMessage> messages) {
            var found = new HashSet<String>();
            for (ConversationMessage msg : messages) {
                Matcher m = MED_PATTERN.matcher(msg.getContent());
                while (m.find()) {
                    found.add(m.group(1).toLowerCase());
                }
            }
            return found;
        }
    }

    record AllergyExtractor() implements Extractor {
        private static final Pattern ALLERGY_PATTERN = Pattern.compile(
                "(?i)\\b(allergic to|allergy to|allergies)[\\s:]+([\\w\\s]+?)(?:\\.|,|;|and|but|$)");

        @Override
        public String label() { return "Allergies"; }

        @Override
        public Set<String> extract(List<ConversationMessage> messages) {
            var found = new HashSet<String>();
            for (ConversationMessage msg : messages) {
                Matcher m = ALLERGY_PATTERN.matcher(msg.getContent());
                while (m.find()) {
                    String substance = m.group(2).trim();
                    if (!substance.isBlank() && substance.length() < 50) {
                        found.add(substance);
                    }
                }
            }
            if (found.isEmpty()) {
                found.add("None reported");
            }
            return found;
        }
    }

    record ConditionExtractor() implements Extractor {
        private static final Pattern CONDITION_PATTERN = Pattern.compile(
                "(?i)\\b(" +
                        "diabetes|hypertension|high blood pressure|asthma|arthritis" +
                        "|migraine|eczema|psoriasis|IBS|GERD|acid reflux" +
                        "|anemia|thyroid|hypothyroidism|hyperthyroidism" +
                        "|COPD|bronchitis|sinusitis|allergy|allergies" +
                        "|depression|anxiety|PTSD|ADHD|insomnia" +
                        "|heart disease|cholesterol|obesity" +
                        ")\\b");

        @Override
        public String label() { return "Medical conditions"; }

        @Override
        public Set<String> extract(List<ConversationMessage> messages) {
            var found = new HashSet<String>();
            for (ConversationMessage msg : messages) {
                Matcher m = CONDITION_PATTERN.matcher(msg.getContent());
                while (m.find()) {
                    found.add(m.group(1).toLowerCase());
                }
            }
            return found;
        }
    }
}
