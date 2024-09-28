package org.example;

import org.jpl7.Query;
import org.jpl7.Term;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputHandler {
    /*
    Расы на выбор:
    аргонианин, каджит, редгард, норд, бретон, имперец, высокий_эльф, лесной_эльф, темный_эльф, орк.

    Классы на выбор:
    варвар, солдат, паладин, ассасин, лучник, алхимик, маг, некромант, псионик.

    Навыки на выбор:
    одноручное_оружие, двуручное_оружие, блокирование, тяжелая_броня, кузнечное_дело, зачарование, взлом, красноречие,
    стрельба, легкая_броня, алхимия, скрытность, карманные_кражи, разрушение, восстановление, колдовство, иллюзия.

    Созвездия на выбор:
    воин, атронах, конь, леди, любовник, маг, вор, лорд, башня.

    Фракции гражданской войны:
    Братья_бури, Имерия

    Я хочу играть за [раса]/ или [раса]. Какие классы мне подойдут?
    Я хочу играть за [класс]/ и [класс]. Что мне нужно знать?
    Я поддерживаю [фракция]. Где я могу присоединиться к ним?

    Я хочу играть за маг и ассасин. Что мне нужно знать?
     Я хочу играть за каджит или орк. Какие классы мне подойдут?
     Я поддерживаю Империя. Где я могу присоединиться к ним?
    * */
    private Scanner sc;
    private HashMap<String, List<String>> facts;

    public InputHandler() {
        String prologFile = "consult('src/main/java/org/example/lab2_prolog.pl')";
        Query consultQuery = new Query(prologFile);
        sc = new Scanner(System.in);
        consultQuery.hasSolution();

        facts = importFacts();
        System.out.println("Расы на выбор:\n" +
                "    аргонианин, каджит, редгард, норд, бретон, имперец, высокий_эльф, лесной_эльф, темный_эльф, орк.");
        System.out.println("Классы на выбор:\n" +
                "    варвар, солдат, паладин, ассасин, лучник, алхимик, маг, некромант, псионик.");
        System.out.println("Фракции гражданской войны:\n" +
                "    Братья_бури, Империя");
        System.out.println("Не изменяйте падеж существительных и пишите их в именительном.\n");
        System.out.println("Пример запросов:\n" +
                "Я хочу играть за [раса]/ или [раса]. Какие классы мне подойдут?\n" +
                "Я хочу играть за [класс]/ и [класс]. Что мне нужно знать?\n" +
                "Я поддерживаю [фракция]. Где я могу присоединиться к ним?\n");
    }

    private List<String> toList(Term result) {
        String line = result.toString();
        line = line.replaceAll("[\\[\\]']", "");
        return List.of(line.split(", "));
    }

    private HashMap<String, List<String>> importFacts() {
        HashMap<String, List<String>> facts = new HashMap<>();
        String[] prologQueries = {"Фракция", "Раса", "Класс", "Навык", "Созвездие"};
        for (String fact : prologQueries) {
            String prologQuery = "findall(" + fact + ", " + fact.toLowerCase() + "(" + fact + ")" + ", " + "Список).";
            Query query = new Query(prologQuery);
            Term result = query.oneSolution().get("Список");
            facts.put(fact, toList(result));
        }
        return facts;
    }

    public void recommendationStart() {
        String[] regexes = {"Я хочу играть за ([А-Яа-я]+)(?: или ([А-Яа-я]+))?\\. Какие классы мне подойдут\\?",
                "Я хочу играть за ([А-Яа-я]+)(?: и ([А-Яа-я]+))?\\. Что мне нужно знать\\?",
                "Я поддерживаю ([А-Яа-я_]+)\\. Где я могу присоединиться к ним\\?"};

        sc = new Scanner(System.in);
        String userLine;
        try {
            while (true) {
                boolean flag = true;
                userLine = sc.nextLine();
                for (int i = 0; i < 3; i++) {
                    Pattern pattern = Pattern.compile(regexes[i], Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(userLine);
                    if (matcher.matches()) {
                        switch (i) {
                            case 0:
                                raceHandler(matcher);
                                break;
                            case 1:
                                classHandler(matcher);
                                break;
                            case 2:
                                warHandler(matcher);
                                break;
                        }
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    System.out.println("Неизвестный запрос.\n");
                }

            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

    }

    private void warHandler(Matcher matcher) {
        String prologQuery;
        String arg = "Владение";
        switch (matcher.group(1).toLowerCase()) {
            case "империя":
                prologQuery = "оплот_империи(" + arg + ").";
                break;
            case "братья_бури":
                prologQuery = "оплот_братьев_бури(" + arg + ").";
                break;
            default:
                System.out.println("В гражданской войне участвуют только братья_бури и империя.\n");
                return;
        }
        Query query = new Query(prologQuery);
        HashSet<String> regions = getAllAnswers(query, arg);
        System.out.printf("Вам стоит посетить следующие владения: %s.\n", String.join(", ", regions));
        System.out.println("Их ярлы поддерживают данную идеологию.\n");
    }

    private void raceHandler(Matcher matcher) {
        int count = 1;
        while (true) {
            try {
                if (matcher.group(count) != null) {
                    boolean flag = true;
                    String chosenRace = matcher.group(count).toLowerCase();
                    for (String race : facts.get("Раса")) {
                        if (chosenRace.equals(race.toLowerCase().trim())) {
                            String prologQuery = "рекомендации_по_расе(" + chosenRace + ", Список).";
                            Query query = new Query(prologQuery);
                            Term result = query.oneSolution().get("Список");
                            List<String> classes = toList(result);
                            System.out.printf("Для расы %s подойдут следущие игровые классы: %s.\n", chosenRace, String.join(", ", classes));
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        System.out.println(count + ": неизвестная раса");
                    }
                    count++;

                }
            } catch (IndexOutOfBoundsException e) {
                System.out.println();
                return;
            }


        }

    }

    private void classHandler(Matcher matcher) {
        int count = 1;
        HashMap<String, Integer> theBestRaces = new HashMap<String, Integer>();
        HashSet<String> races = new HashSet<>();
        HashSet<String> fractions = new HashSet<>();
        HashSet<String> skills = new HashSet<>();
        HashSet<String> stones = new HashSet<>();
        while (true) {
            try {
                if (matcher.group(count) != null) {
                    boolean flag = true;
                    String chosenClass = matcher.group(count).toLowerCase();
                    for (String gameClass : facts.get("Класс")) {
                        if (chosenClass.equals(gameClass.toLowerCase().trim())) {
                            recRace(chosenClass, races, theBestRaces);
                            recFraction(chosenClass, fractions);
                            recSkill(chosenClass, skills);
                            recStone(chosenClass, stones);

                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        System.out.println(count + ": неизвестный класс");
                    }
                    count++;
                }
            } catch (IndexOutOfBoundsException e) {
                ArrayList<String> theBestChoice = new ArrayList<>();
                for (String race : theBestRaces.keySet()) {
                    if (theBestRaces.get(race) == 2) {
                        theBestChoice.add(race);
                    }
                }
                if (!theBestChoice.isEmpty()) {
                    System.out.printf("При данном подходе к игре вам рекомендовано выбрать следующие расы: %s.\n", String.join(", ", theBestChoice));
                }
                if (!races.isEmpty()) {
                    theBestChoice.forEach(races::remove);
                    System.out.printf("Расы, которые частично подходят для выбранных классов: %s.\n", String.join(", ", races));
                }
                if (!fractions.isEmpty()) {
                    System.out.printf("Также для вас будут доступны квесты следующих фракций: %s. Обязательно их посетите!\n", String.join(", ", fractions));
                }
                if (!skills.isEmpty()) {
                    System.out.printf("Для лучшего отыгрыша вам пригодятся следующие навыки: %s. " +
                            "Они будут прокачиваться самостоятельно по мере соответсвующей игры, " +
                            "либо их стоит учить намеренно у учителей или по книгам.\n", String.join(", ", skills));
                }
                if (!stones.isEmpty()) {
                    System.out.printf("Для ускорения прогресса вам рекомендовано активировать один из следующих камней-хранителей: %s.\n", String.join(", ", stones));
                }
                System.out.println();
                return;
            }


        }
    }

    private void recRace(String chosenClass, HashSet<String> races, HashMap<String, Integer> theBestRaces) {
        String prologQuery = "рекомендации_по_классу_раса(" + chosenClass + ", Список).";
        Query query = new Query(prologQuery);
        Term result = query.oneSolution().get("Список");
        List<String> list = toList(result);
        races.addAll(list);
        for (String race : list) {
            if (theBestRaces.containsKey(race)) {
                theBestRaces.put(race, theBestRaces.get(race) + 1);
            } else {
                theBestRaces.put(race, 1);
            }
        }
    }

    private void recFraction(String chosenClass, HashSet<String> fractions) {
        String prologQuery = "рекомендации_по_классу_фракция(" + chosenClass + ", Список).";
        Query query = new Query(prologQuery);
        Term result = query.oneSolution().get("Список");
        List<String> list = toList(result);
        fractions.addAll(list);
    }

    private void recSkill(String chosenClass, HashSet<String> skills) {
        String prologQuery = "рекомендации_по_классу_навык(" + chosenClass + ", Список).";
        Query query = new Query(prologQuery);
        Term result = query.oneSolution().get("Список");
        List<String> list = toList(result);
        skills.addAll(list);
    }

    private void recStone(String chosenClass, HashSet<String> stones) {
        String prologQuery = "рекомендации_по_классу_созвездие(" + chosenClass + ", Список).";
        Query query = new Query(prologQuery);
        Term result = query.oneSolution().get("Список");
        List<String> list = toList(result);
        stones.addAll(list);
    }


    private HashSet<String> getAllAnswers(Query query, String arg) {
        HashSet<String> answers = new HashSet<>();
        while (query.hasMoreSolutions()) {
            Term answer = query.nextSolution().get(arg);
            answers.add(answer.toString().replaceAll("'", "").trim());
        }
        return answers;
    }
}
