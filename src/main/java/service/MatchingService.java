package service;

import model.Person;
import model.LostPerson;
import model.FoundPerson;
import model.MatchRecord;
import java.util.List;
import java.util.ArrayList;

public class MatchingService {

    private static final int AGE_TOLERANCE_YEARS = 3;
    private static final double DISTANCE_TOLERANCE_KM = 50.0;
    private static final double NAME_SIMILARITY_THRESHOLD = 60.0;


    private static final double PARTIAL_NAME_MATCH_PENALTY = 0.7;

    private int nextMatchId = 1;

    // compares every LostPerson against every FoundPerson
    public List<MatchRecord> findMatches(List<Person> allPersons) {
        List<LostPerson> lostPersons = filterLost(allPersons);
        List<FoundPerson> foundPersons = filterFound(allPersons);

        List<MatchRecord> matches = new ArrayList<MatchRecord>();

        for (LostPerson lost : lostPersons) {
            for (FoundPerson found : foundPersons) {
                double score = calculateConfidence(lost, found);
                if (score > 0) {
                    matches.add(new MatchRecord(nextMatchId, lost, found, score));
                    nextMatchId++;
                }
            }
        }

        return matches;
    }

    private List<LostPerson> filterLost(List<Person> persons) {
        List<LostPerson> filtered = new ArrayList<LostPerson>();
        for (Person person : persons) {
            if (person instanceof LostPerson) {
                filtered.add((LostPerson) person);
            }
        }
        return filtered;
    }

    private List<FoundPerson> filterFound(List<Person> persons) {
        List<FoundPerson> filtered = new ArrayList<FoundPerson>();
        for (Person person : persons) {
            if (person instanceof FoundPerson) {
                filtered.add((FoundPerson) person);
            }
        }
        return filtered;
    }

    private double calculateConfidence(Person missing, Person found) {
        double score = 0;

        double nameSimilarity = calculateNameSimilarity(missing.getName(), found.getName());
        if (nameSimilarity < NAME_SIMILARITY_THRESHOLD) {
            return 0;
        }
        score += (nameSimilarity / 100.0) * 50;

        int ageDifference = Math.abs(missing.getAge() - found.getAge());
        if (ageDifference <= AGE_TOLERANCE_YEARS) {
            score += 25;
        }

        if (missing.getGender().equalsIgnoreCase(found.getGender())) {
            score += 10;
        }

        double distance = missing.getLocation().distanceTo(found.getLocation());
        if (distance <= DISTANCE_TOLERANCE_KM) {
            score += 15;
        }

        return score;
    }

//name comparison
    private double calculateNameSimilarity(String name1, String name2) {
        String a = name1.toLowerCase().trim();
        String b = name2.toLowerCase().trim();

        String[] tokensA = a.split("\\s+");
        String[] tokensB = b.split("\\s+");

        double fullNameSimilarity = stringSimilarity(a, b);
        double bestTokenSimilarity = bestTokenSimilarity(tokensA, tokensB);

        if (tokensA.length != tokensB.length) {
            bestTokenSimilarity *= PARTIAL_NAME_MATCH_PENALTY;
        }

        return Math.max(fullNameSimilarity, bestTokenSimilarity);
    }


    private double bestTokenSimilarity(String[] tokensA, String[] tokensB) {
        double best = 0;
        for (String tokenA : tokensA) {
            for (String tokenB : tokensB) {
                double sim = stringSimilarity(tokenA, tokenB);
                if (sim > best) {
                    best = sim;
                }
            }
        }
        return best;
    }

    private double stringSimilarity(String a, String b) {
        int distance = levenshteinDistance(a, a.length(), b, b.length());
        int maxLength = Math.max(a.length(), b.length());

        if (maxLength == 0) {
            return 100;
        }

        return (1.0 - ((double) distance / maxLength)) * 100;
    }

    private int levenshteinDistance(String a, int lengthA, String b, int lengthB) {
        if (lengthA == 0) {
            return lengthB;
        }
        if (lengthB == 0) {
            return lengthA;
        }

        if (a.charAt(lengthA - 1) == b.charAt(lengthB - 1)) {
            return levenshteinDistance(a, lengthA - 1, b, lengthB - 1);
        }

        int insert = levenshteinDistance(a, lengthA, b, lengthB - 1);
        int delete = levenshteinDistance(a, lengthA - 1, b, lengthB);
        int substitute = levenshteinDistance(a, lengthA - 1, b, lengthB - 1);

        return 1 + Math.min(insert, Math.min(delete, substitute));
    }
}