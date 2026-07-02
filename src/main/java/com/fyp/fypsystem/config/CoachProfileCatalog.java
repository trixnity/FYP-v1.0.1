package com.fyp.fypsystem.config;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CoachProfileCatalog {

    public record CoachProfile(
            String slug,
            String username,
            String name,
            String email,
            String title,
            Integer rating,
            String achievement,
            String background,
            String teachingStyle,
            String classType,
            String profilePicture
    ) {}

    private final List<CoachProfile> profiles = List.of(
            new CoachProfile(
                    "hafiz",
                    "coach_hafiz",
                    "Coach Hafiz",
                    "coach_hafiz@educhess.demo",
                    "National Representative Coach",
                    2200,
                    "Malaysian representative. Bronze Medallist for Asean University Games 2023. Former Terengganu player since 2011.",
                    "Experienced state and university chess competitor with long-term tournament preparation experience.",
                    "Structured calculation, opening discipline, and practical tournament habits.",
                    "Tournament training / school chess",
                    "assets/coaches/hafiz.jpg"
            ),
            new CoachProfile(
                    "syazrin",
                    "coach_syazrin",
                    "Coach Syazrin",
                    "coach_syazrin@educhess.demo",
                    "Team Event Specialist",
                    2100,
                    "Sukma Best Board Team 2024. GACC Team Champion 2021. Mid Valley Armageddon Blitz Champion 2023.",
                    "Team chess and blitz specialist with strong competitive experience across university and open events.",
                    "Fast pattern recognition, active piece play, and confidence in practical positions.",
                    "Online coaching / tournament training",
                    "assets/coaches/syazrin.jpg"
            ),
            new CoachProfile(
                    "ali",
                    "coach_ali",
                    "Coach Ali",
                    "coach_ali@educhess.demo",
                    "Classical and Streaming Coach",
                    2050,
                    "RD Open Seremban Runner-up 2024. Defending Champion for Kelantan Closed since 2022. Official Chess Streamer.",
                    "Competitive player and chess content creator who explains ideas clearly for developing players.",
                    "Interactive analysis, clear verbal explanation, and practical game review.",
                    "Beginner learning / online coaching",
                    "assets/coaches/ali.jpg"
            ),
            new CoachProfile(
                    "omar",
                    "coach_omar",
                    "Coach Omar",
                    "coach_omar@educhess.demo",
                    "Rapid Chess Coach",
                    2000,
                    "KL Master Rapid Champion 2023. Runner-up Sukma Duo 2024. Part of AUG-elect 2022.",
                    "Rapid and team-event competitor focused on decision-making under time pressure.",
                    "Time management, candidate moves, and simple plans from common structures.",
                    "School chess / tournament training",
                    "assets/coaches/omar.jpg"
            ),
            new CoachProfile(
                    "syakir",
                    "coach_syakir",
                    "Coach Syakir",
                    "coach_syakir@educhess.demo",
                    "Classical Champion Coach",
                    2300,
                    "National Closed Classical Champion 2024. Chess 360 Tour Champion 2024. Finalist Asean Youth Chess Championship.",
                    "High-level classical player with national and youth championship experience.",
                    "Deep calculation, positional planning, and disciplined post-game analysis.",
                    "Tournament training",
                    "assets/coaches/syakir.jpg"
            ),
            new CoachProfile(
                    "nabil",
                    "coach_nabil",
                    "Coach Nabil",
                    "coach_nabil@educhess.demo",
                    "Development Coach",
                    1850,
                    "Former Pahang Player. 2nd Runner-up Pekan Open 2023. Top-10 Pantai Timur Open 2022 Challenger Category.",
                    "State-level player with experience guiding improvers through local competition preparation.",
                    "Fundamentals, tactical awareness, and steady improvement routines.",
                    "Beginner learning / school chess",
                    "assets/coaches/nabil.jpg"
            ),
            new CoachProfile(
                    "amzar",
                    "coach_amzar",
                    "Coach Amzar",
                    "coach_amzar@educhess.demo",
                    "Team and IPT Coach",
                    1900,
                    "Mesamall Nilai Duo Runner-up 2022. Top-10 Merdeka Open Team 2023. Best IPT PMOCT 2022 Open Category.",
                    "Team event and IPT competitor familiar with student training needs.",
                    "Collaborative review, opening preparation, and practical middlegame plans.",
                    "School chess / online coaching",
                    "assets/coaches/amzar.jpg"
            ),
            new CoachProfile(
                    "yeng",
                    "coach_yeng",
                    "Coach Yeng",
                    "coach_yeng@educhess.demo",
                    "Youth Champion Coach",
                    2150,
                    "Asean Age Group Champion 2022. Former State Player since 2018. Official Chess Streamer.",
                    "Youth champion and content creator with state-player experience.",
                    "Energetic lessons, tactical themes, and clear examples from real games.",
                    "Beginner learning / online coaching",
                    "assets/coaches/yeng.jpg"
            ),
            new CoachProfile(
                    "ikmal",
                    "coach_ikmal",
                    "Coach Ikmal",
                    "coach_ikmal@educhess.demo",
                    "Interstate Coach",
                    2000,
                    "Johor Interstate Champion 2022. KL Gateway Medallist 2024. Former Kelantan Champion since 2014.",
                    "Long-time state champion with interstate and open event results.",
                    "Competitive preparation, resilient defense, and conversion technique.",
                    "Tournament training / school chess",
                    "assets/coaches/ikmal.jpg"
            )
    );

    public List<CoachProfile> profiles() {
        return profiles;
    }

    public Optional<CoachProfile> byEmail(String email) {
        return profiles.stream().filter(profile -> profile.email().equalsIgnoreCase(email)).findFirst();
    }

    public boolean isOfficialCoachEmail(String email) {
        return byEmail(email).isPresent();
    }
}
