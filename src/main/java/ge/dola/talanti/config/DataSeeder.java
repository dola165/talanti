package ge.dola.talanti.config;

import ge.dola.talanti.club.ClubCommunicationMethod;
import ge.dola.talanti.club.ClubDynamicTables;
import ge.dola.talanti.club.ClubRole;
import ge.dola.talanti.user.UserType;
import org.jooq.DSLContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.*;

@Component
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final DSLContext dsl;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(DSLContext dsl, PasswordEncoder passwordEncoder) {
        this.dsl = dsl;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("Ensuring Talanti development seed data...");
        String commonPasswordHash = passwordEncoder.encode("password");
        String dinamoBannerUrl = "/uploads/seed-media/dinamo-banner.svg";
        String dinamoLogoUrl = "/uploads/seed-media/dinamo-logo.svg";
        String strikerAvatarUrl = "/uploads/seed-media/player-striker.svg";
        String midfielderAvatarUrl = "/uploads/seed-media/player-midfielder.svg";
        String keeperAvatarUrl = "/uploads/seed-media/player-keeper.svg";
        String defenderAvatarUrl = "/uploads/seed-media/player-defender.svg";
        String trainingPostMediaUrl = "/uploads/seed-media/post-training.svg";
        String trophyPostMediaUrl = "/uploads/seed-media/post-trophy.svg";
        LocalDate seedBaseDate = LocalDate.now();

        Long lukaId = createUser("coach_luka", "coach@talanti.ge", UserType.CLUB_ADMIN, commonPasswordHash,
                "Coach Luka", "UEFA A License. Developing the next generation of Georgian talent.");
        Long mariamId = createUser("mariam_coach", "mariam@talanti.ge", UserType.CLUB_ADMIN, commonPasswordHash,
                "Mariam Abashidze", "Head coach focused on girls development pathways.");
        Long nikaId = createUser("nika_admin", "nika@talanti.ge", UserType.CLUB_ADMIN, commonPasswordHash,
                "Nika Chikovani", "Academy director building high-performance youth structures.");
        Long anaId = createUser("ana_admin", "ana@talanti.ge", UserType.CLUB_ADMIN, commonPasswordHash,
                "Ana Tsereteli", "Club manager supporting grassroots access and scouting.");
        Long gioId = createUser("gio_admin", "gio@talanti.ge", UserType.CLUB_ADMIN, commonPasswordHash,
                "Gio Meskhi", "Community club founder and match organizer.");

        Long playerOne = createUser("giorgi_st", "giorgi@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Giorgi Beridze", "Direct forward with pressing intensity.");
        Long playerTwo = createUser("sandro_cm", "sandro@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Sandro Kobalia", "Midfielder with strong range of passing.");
        Long playerThree = createUser("nini_gk", "nini@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Nini Davitadze", "Goalkeeper leading from the back.");
        Long playerFour = createUser("lasha_cb", "lasha@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Lasha Japaridze", "Center-back comfortable in build-up play.");
        Long playerFive = createUser("beka_rw", "beka@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Beka Mchedlishvili", "Left-footed winger stretching defensive lines.");
        Long playerSix = createUser("tornike_lb", "tornike@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Tornike Gelashvili", "Aggressive full-back with recovery speed.");
        Long playerSeven = createUser("elene_st", "elene@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Elene Kapanadze", "Clinical striker finishing quickly in the box.");
        Long playerEight = createUser("mari_cm", "mari@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Mari Khvedelidze", "Controller in midfield with calm distribution.");
        Long playerNine = createUser("saba_gk", "saba@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Saba Kbilashvili", "Shot-stopping goalkeeper commanding the line.");
        Long playerTen = createUser("irakli_cb", "irakli@talanti.ge", UserType.PLAYER, commonPasswordHash,
                "Irakli Maisuradze", "Organizer at center-back and vocal leader.");

        Long dinamoLocation = createLocation("Boris Paichadze Dinamo Arena, Tbilisi", 41.7151, 44.8271);
        Long saburtaloLocation = createLocation("Bendela Stadium, Tbilisi", 41.7326, 44.7462);
        Long batumiLocation = createLocation("Batumi Football Centre, Batumi", 41.6168, 41.6367);
        Long kutaisiLocation = createLocation("Ramaz Shengelia Stadium, Kutaisi", 42.2679, 42.6946);
        Long rustaviLocation = createLocation("Rustavi Technical Centre, Rustavi", 41.5495, 44.9930);

        Long dinamoId = createClub(lukaId, "FC Dinamo Tbilisi Academy", "ACADEMY", dinamoLocation,
                "Elite youth setup with strong U16 and U18 development tracks.");
        Long saburtaloId = createClub(nikaId, "Saburtalo Youth Lab", "ACADEMY", saburtaloLocation,
                "Progressive academy focused on technical football and youth scouting.");
        Long batumiId = createClub(mariamId, "Batumi Waves FC", "GRASSROOTS", batumiLocation,
                "Coastal club with a fast-growing girls pathway and open trials.");
        Long kutaisiId = createClub(anaId, "Kutaisi Phoenix", "PROFESSIONAL", kutaisiLocation,
                "Senior club recruiting locally and hosting regular open sessions.");
        Long rustaviId = createClub(gioId, "Rustavi Steel Juniors", "GRASSROOTS", rustaviLocation,
                "Community-first academy serving early age groups and mixed programming.");

        updateClubMedia(dinamoId, dinamoLogoUrl, dinamoBannerUrl);
        updateClubMedia(saburtaloId, dinamoLogoUrl, dinamoBannerUrl);
        updateClubMedia(batumiId, dinamoLogoUrl, dinamoBannerUrl);
        updateClubMedia(kutaisiId, dinamoLogoUrl, dinamoBannerUrl);
        updateClubMedia(rustaviId, dinamoLogoUrl, dinamoBannerUrl);
        updateClubContacts(dinamoId, "academy@dinamo.ge", "+995577100101", "https://m.me/fcdinamoacademy", ClubCommunicationMethod.WHATSAPP);
        updateClubContacts(saburtaloId, "hello@saburtalolab.ge", "+995577100102", "https://m.me/saburtaloyouthlab", ClubCommunicationMethod.FACEBOOK_MESSENGER);
        updateClubContacts(batumiId, "contact@batumiwaves.ge", "+995577100103", null, ClubCommunicationMethod.WHATSAPP);
        updateClubContacts(kutaisiId, "info@kutaisiphoenix.ge", "+995577100104", "https://m.me/kutaisiphoenix", ClubCommunicationMethod.WHATSAPP);
        updateClubContacts(rustaviId, "club@rustavisteel.ge", "+995577100105", null, ClubCommunicationMethod.WHATSAPP);

        updateUserPresentation(lukaId, dinamoLogoUrl);
        updateUserPresentation(mariamId, dinamoLogoUrl);
        updateUserPresentation(nikaId, dinamoLogoUrl);
        updateUserPresentation(anaId, dinamoLogoUrl);
        updateUserPresentation(gioId, dinamoLogoUrl);
        updateUserPresentation(playerOne, strikerAvatarUrl);
        updateUserPresentation(playerTwo, midfielderAvatarUrl);
        updateUserPresentation(playerThree, keeperAvatarUrl);
        updateUserPresentation(playerFour, defenderAvatarUrl);
        updateUserPresentation(playerFive, midfielderAvatarUrl);
        updateUserPresentation(playerSix, defenderAvatarUrl);
        updateUserPresentation(playerSeven, strikerAvatarUrl);
        updateUserPresentation(playerEight, midfielderAvatarUrl);
        updateUserPresentation(playerNine, keeperAvatarUrl);
        updateUserPresentation(playerTen, defenderAvatarUrl);

        upsertPlayerDetails(playerOne, "STRIKER", "RIGHT", 181, 74, "AVAILABLE");
        upsertPlayerDetails(playerTwo, "MIDFIELDER", "RIGHT", 178, 72, "AVAILABLE");
        upsertPlayerDetails(playerThree, "GOALKEEPER", "RIGHT", 176, 68, "AVAILABLE");
        upsertPlayerDetails(playerFour, "CENTER_BACK", "RIGHT", 185, 79, "AVAILABLE");
        upsertPlayerDetails(playerFive, "WINGER", "LEFT", 174, 67, "AVAILABLE");
        upsertPlayerDetails(playerSix, "LEFT_BACK", "RIGHT", 177, 71, "AVAILABLE");
        upsertPlayerDetails(playerSeven, "STRIKER", "RIGHT", 170, 61, "AVAILABLE");
        upsertPlayerDetails(playerEight, "MIDFIELDER", "RIGHT", 169, 60, "AVAILABLE");
        upsertPlayerDetails(playerNine, "GOALKEEPER", "LEFT", 183, 76, "AVAILABLE");
        upsertPlayerDetails(playerTen, "CENTER_BACK", "RIGHT", 186, 80, "AVAILABLE");

        Long dinamoSquad = createSquad(dinamoId, "U16 Boys", "U16", "MALE", lukaId);
        Long dinamoEliteSquad = createSquad(dinamoId, "Dinamo Youth A", "U19", "MALE", lukaId);
        Long dinamoGirlsSquad = createSquad(dinamoId, "U16 Girls", "U16", "FEMALE", lukaId);
        Long saburtaloSquad = createSquad(saburtaloId, "U14 Boys", "U14", "MALE", nikaId);
        Long saburtaloEliteSquad = createSquad(saburtaloId, "Technical Core", "U16", "MALE", nikaId);
        Long batumiSquad = createSquad(batumiId, "U17 Girls", "U17", "FEMALE", mariamId);
        Long kutaisiSquad = createSquad(kutaisiId, "First Team", "SENIOR", "MALE", anaId);
        Long rustaviSquad = createSquad(rustaviId, "U12 Mixed", "U12", "MIXED", gioId);

        addSquadPlayer(dinamoSquad, playerOne, 9);
        addSquadPlayer(dinamoSquad, playerTwo, 8);
        addSquadPlayer(dinamoSquad, playerSix, 3);
        addSquadPlayer(dinamoEliteSquad, playerFive, 11);
        addSquadPlayer(dinamoEliteSquad, playerTen, 4);
        addSquadPlayer(dinamoGirlsSquad, playerSeven, 9);
        addSquadPlayer(dinamoGirlsSquad, playerEight, 8);
        addSquadPlayer(batumiSquad, playerThree, 1);
        addSquadPlayer(saburtaloSquad, playerNine, 1);
        addSquadPlayer(saburtaloEliteSquad, playerTwo, 6);
        addSquadPlayer(kutaisiSquad, playerFour, 4);

        seedOpportunity(dinamoId, "FUNDRAISING", "New Training Equipment Fund",
                "Help us buy new GPS vests for the U16 squad.", "https://gofundme.com/example-dinamo");
        seedOpportunity(dinamoId, "JOB", "Job Opportunities",
                "Recruiting a part-time performance analyst to support academy match review.", "https://example.com/dinamo-performance-analyst");
        seedOpportunity(dinamoId, "VOLUNTEER", "Volunteer Opportunities",
                "Seeking matchday and community-event volunteers for academy weekends.", "https://example.com/dinamo-volunteer");
        seedOpportunity(dinamoId, "WISHLIST", "Wish List",
                "Portable goals, recovery bands, and spare goalkeeper gloves for training blocks.", "https://example.com/dinamo-wishlist");
        seedOpportunity(saburtaloId, "JOB", "Volunteer Analyst Wanted",
                "Join the academy staff to help break down match footage.", "https://example.com/saburtalo-analyst");
        seedOpportunity(batumiId, "WISHLIST", "Goalkeeper Gloves Drive",
                "Collecting quality gloves and cones for our girls programme.", "https://example.com/batumi-wishlist");

        seedHonour(dinamoId, "Academy Cup Champions", 2025, "Won the national academy tournament after a seven-match unbeaten run.");
        seedHonour(dinamoId, "Tbilisi Youth Super Cup", 2024, "Lifted the regional super cup with the U16 group.");
        seedHonour(dinamoId, "Best Development Programme", 2023, "Recognized for year-round player pathway delivery and education support.");
        seedHonour(saburtaloId, "Technical Development Award", 2024, "Awarded for academy methodology and coach education.");
        seedTrustLink(kutaisiId, dinamoId, lukaId);
        seedTrustLink(batumiId, saburtaloId, nikaId);

        seedTryout(dinamoId, dinamoLocation, lukaId, "Dinamo U16 Open Tryout", "U16", "MIDFIELDER", seedBaseDate.plusDays(7).atTime(18, 0));
        seedTryout(saburtaloId, saburtaloLocation, nikaId, "Saburtalo U14 Technical Trial", "U14", "ATTACKER", seedBaseDate.plusDays(10).atTime(17, 30));
        seedTryout(batumiId, batumiLocation, mariamId, "Batumi Girls Trial Session", "U17", "GOALKEEPER", seedBaseDate.plusDays(14).atTime(18, 30));
        seedTryout(kutaisiId, kutaisiLocation, anaId, "Kutaisi Senior Assessment", "SENIOR", "DEFENDER", seedBaseDate.plusDays(18).atTime(19, 0));

        seedMatch(dinamoId, saburtaloId, dinamoLocation, seedBaseDate.plusDays(5).atTime(19, 0), "OPEN", "FRIENDLY");
        seedMatch(kutaisiId, batumiId, kutaisiLocation, seedBaseDate.plusDays(12).atTime(20, 0), "OPEN", "COMPETITIVE");
        seedMatch(rustaviId, dinamoId, rustaviLocation, seedBaseDate.plusDays(20).atTime(18, 30), "PENDING_ACCEPTANCE", "FRIENDLY");

        seedAvailability(dinamoId, seedBaseDate.plusDays(9), "Looking for U16/U17 friendly opponents. Travel possible.");
        seedAvailability(batumiId, seedBaseDate.plusDays(16), "Girls squad available for weekend fixtures.");
        seedAvailability(rustaviId, seedBaseDate.plusDays(22), "Open to hosting grassroots friendlies.");

        Long dinamoTrainingPost = seedPost(lukaId, dinamoId,
                "Saturday micro-cycle locked in. U16 and Youth A squads will combine for an accelerated finishing block before the next friendly.");
        attachMediaToPost(dinamoTrainingPost, trainingPostMediaUrl, lukaId);

        Long dinamoHonourPost = seedPost(lukaId, dinamoId,
                "Archive update: our academy honour wall now includes the Academy Cup and the Tbilisi Youth Super Cup. Standards stay high.");
        attachMediaToPost(dinamoHonourPost, trophyPostMediaUrl, lukaId);

        seedPost(nikaId, saburtaloId, "Saburtalo Youth Lab is opening an extra technical session for U14 midfielders and one-on-one development plans.");
        seedPost(mariamId, batumiId, "Batumi Waves are hosting girls-only trials next month. Goalkeepers especially welcome.");
        seedPost(anaId, kutaisiId, "Phoenix are reviewing senior full-backs and center-backs for the next window.");
        seedPost(gioId, rustaviId, "Rustavi Steel Juniors are building their next U12 intake with mixed grassroots sessions.");

        System.out.println("Talanti development seed data ensured.");
    }

    private Long createUser(String username, String email, UserType type, String passwordHash, String fullName, String bio) {
        Long existingUserId = dsl.select(USERS.ID)
                .from(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOneInto(Long.class);

        if (existingUserId != null) {
            return existingUserId;
        }

        Long userId = dsl.insertInto(USERS)
                .set(USERS.USERNAME, username)
                .set(USERS.EMAIL, email)
                .set(USERS.PASSWORD_HASH, passwordHash)
                .set(USERS.USER_TYPE, type.name())
                .set(USERS.CREATED_AT, LocalDateTime.now())
                .returningResult(USERS.ID)
                .fetchOneInto(Long.class);

        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, userId)
                .set(USER_PROFILES.FULL_NAME, fullName)
                .set(USER_PROFILES.BIO, bio)
                .execute();

        if (type == UserType.PLAYER) {
            dsl.insertInto(PLAYER_DETAILS)
                    .set(PLAYER_DETAILS.USER_ID, userId)
                    .set(PLAYER_DETAILS.PRIMARY_POSITION, "MIDFIELDER")
                    .set(PLAYER_DETAILS.PREFERRED_FOOT, "RIGHT")
                    .set(PLAYER_DETAILS.HEIGHT_CM, 178)
                    .set(PLAYER_DETAILS.WEIGHT_KG, 72)
                    .set(PLAYER_DETAILS.AVAILABILITY_STATUS, "AVAILABLE")
                    .execute();
        }

        return userId;
    }

    private Long createLocation(String address, double latitude, double longitude) {
        Long existingLocationId = dsl.select(LOCATIONS.ID)
                .from(LOCATIONS)
                .where(LOCATIONS.ADDRESS_TEXT.eq(address))
                .fetchOneInto(Long.class);

        if (existingLocationId != null) {
            return existingLocationId;
        }

        return dsl.insertInto(LOCATIONS)
                .set(LOCATIONS.ADDRESS_TEXT, address)
                .set(LOCATIONS.LATITUDE, BigDecimal.valueOf(latitude))
                .set(LOCATIONS.LONGITUDE, BigDecimal.valueOf(longitude))
                .set(LOCATIONS.CREATED_AT, LocalDateTime.now())
                .returningResult(LOCATIONS.ID)
                .fetchOneInto(Long.class);
    }

    private Long createClub(Long ownerId, String name, String type, Long locationId, String description) {
        Long existingClubId = dsl.select(CLUBS.ID)
                .from(CLUBS)
                .where(CLUBS.NAME.eq(name))
                .fetchOneInto(Long.class);

        if (existingClubId != null) {
            ensureMembership(existingClubId, ownerId, ClubRole.OWNER.name());
            return existingClubId;
        }

        Long clubId = dsl.insertInto(CLUBS)
                .set(CLUBS.NAME, name)
                .set(CLUBS.DESCRIPTION, description)
                .set(CLUBS.TYPE, type)
                .set(CLUBS.STATUS, "VERIFIED")
                .set(CLUBS.LOCATION_ID, locationId)
                .set(CLUBS.CREATED_BY, ownerId)
                .set(CLUBS.CREATED_AT, LocalDateTime.now())
                .returningResult(CLUBS.ID)
                .fetchOneInto(Long.class);

        ensureMembership(clubId, ownerId, ClubRole.OWNER.name());

        return clubId;
    }

    private Long createSquad(Long clubId, String name, String category, String gender, Long coachId) {
        Long existingSquadId = dsl.select(SQUADS.ID)
                .from(SQUADS)
                .where(SQUADS.CLUB_ID.eq(clubId))
                .and(SQUADS.NAME.eq(name))
                .fetchOneInto(Long.class);

        if (existingSquadId != null) {
            return existingSquadId;
        }

        return dsl.insertInto(SQUADS)
                .set(SQUADS.CLUB_ID, clubId)
                .set(SQUADS.NAME, name)
                .set(SQUADS.CATEGORY, category)
                .set(SQUADS.GENDER, gender)
                .set(SQUADS.HEAD_COACH_ID, coachId)
                .set(SQUADS.CREATED_AT, LocalDateTime.now())
                .returningResult(SQUADS.ID)
                .fetchOneInto(Long.class);
    }

    private void addSquadPlayer(Long squadId, Long userId, int jerseyNumber) {
        boolean alreadyLinked = dsl.fetchExists(
                dsl.selectOne()
                        .from(SQUAD_PLAYERS)
                        .where(SQUAD_PLAYERS.SQUAD_ID.eq(squadId))
                        .and(SQUAD_PLAYERS.USER_ID.eq(userId))
        );

        if (alreadyLinked) {
            return;
        }

        dsl.insertInto(SQUAD_PLAYERS)
                .set(SQUAD_PLAYERS.SQUAD_ID, squadId)
                .set(SQUAD_PLAYERS.USER_ID, userId)
                .set(SQUAD_PLAYERS.JERSEY_NUMBER, jerseyNumber)
                .set(SQUAD_PLAYERS.SQUAD_ROLE, "PLAYER")
                .set(SQUAD_PLAYERS.JOINED_AT, LocalDateTime.now())
                .execute();
    }

    private Long seedPost(Long authorId, Long clubId, String content) {
        Long existingPostId = dsl.select(POSTS.ID)
                .from(POSTS)
                .where(POSTS.AUTHOR_ID.eq(authorId))
                .and(POSTS.CLUB_ID.eq(clubId))
                .and(POSTS.CONTENT.eq(content))
                .fetchOneInto(Long.class);

        if (existingPostId != null) {
            return existingPostId;
        }

        return dsl.insertInto(POSTS)
                .set(POSTS.AUTHOR_ID, authorId)
                .set(POSTS.CLUB_ID, clubId)
                .set(POSTS.CONTENT, content)
                .set(POSTS.IS_PUBLIC, true)
                .set(POSTS.CREATED_AT, LocalDateTime.now())
                .returningResult(POSTS.ID)
                .fetchOneInto(Long.class);
    }

    private void seedOpportunity(Long clubId, String type, String title, String description, String externalLink) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_OPPORTUNITIES)
                        .where(CLUB_OPPORTUNITIES.CLUB_ID.eq(clubId))
                        .and(CLUB_OPPORTUNITIES.TITLE.eq(title))
        );

        if (exists) {
            return;
        }

        dsl.insertInto(CLUB_OPPORTUNITIES)
                .set(CLUB_OPPORTUNITIES.CLUB_ID, clubId)
                .set(CLUB_OPPORTUNITIES.TYPE, type)
                .set(CLUB_OPPORTUNITIES.TITLE, title)
                .set(CLUB_OPPORTUNITIES.DESCRIPTION, description)
                .set(CLUB_OPPORTUNITIES.EXTERNAL_LINK, externalLink)
                .set(CLUB_OPPORTUNITIES.STATUS, "OPEN")
                .set(CLUB_OPPORTUNITIES.CREATED_AT, LocalDateTime.now())
                .execute();
    }

    private void seedHonour(Long clubId, String title, Integer yearWon, String description) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_HONOURS)
                        .where(CLUB_HONOURS.CLUB_ID.eq(clubId))
                        .and(CLUB_HONOURS.TITLE.eq(title))
                        .and(CLUB_HONOURS.YEAR_WON.eq(yearWon))
        );

        if (exists) {
            return;
        }

        dsl.insertInto(CLUB_HONOURS)
                .set(CLUB_HONOURS.CLUB_ID, clubId)
                .set(CLUB_HONOURS.TITLE, title)
                .set(CLUB_HONOURS.YEAR_WON, yearWon)
                .set(CLUB_HONOURS.DESCRIPTION, description)
                .execute();
    }

    private void seedTryout(Long clubId, Long locationId, Long createdBy, String title, String ageGroup, String position, LocalDateTime tryoutDate) {
        Long existingTryoutId = dsl.select(TRYOUTS.ID)
                .from(TRYOUTS)
                .where(TRYOUTS.CLUB_ID.eq(clubId))
                .and(TRYOUTS.TITLE.eq(title))
                .fetchOneInto(Long.class);

        if (existingTryoutId != null) {
            dsl.update(TRYOUTS)
                    .set(TRYOUTS.DESCRIPTION, "Open evaluation session for shortlisted players.")
                    .set(TRYOUTS.POSITION, position)
                    .set(TRYOUTS.AGE_GROUP, ageGroup)
                    .set(TRYOUTS.TRYOUT_DATE, tryoutDate)
                    .set(TRYOUTS.DEADLINE, tryoutDate.minusDays(2))
                    .set(TRYOUTS.LOCATION_ID, locationId)
                    .set(TRYOUTS.CREATED_BY, createdBy)
                    .where(TRYOUTS.ID.eq(existingTryoutId))
                    .execute();
            return;
        }

        dsl.insertInto(TRYOUTS)
                .set(TRYOUTS.CLUB_ID, clubId)
                .set(TRYOUTS.TITLE, title)
                .set(TRYOUTS.DESCRIPTION, "Open evaluation session for shortlisted players.")
                .set(TRYOUTS.POSITION, position)
                .set(TRYOUTS.AGE_GROUP, ageGroup)
                .set(TRYOUTS.TRYOUT_DATE, tryoutDate)
                .set(TRYOUTS.DEADLINE, tryoutDate.minusDays(2))
                .set(TRYOUTS.LOCATION_ID, locationId)
                .set(TRYOUTS.CREATED_BY, createdBy)
                .set(TRYOUTS.CREATED_AT, LocalDateTime.now())
                .execute();
    }

    private void seedMatch(Long homeClubId, Long awayClubId, Long locationId, LocalDateTime scheduledDate, String status, String matchType) {
        LocalDateTime dayStart = scheduledDate.toLocalDate().atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);

        Long existingMatchId = dsl.select(MATCHES.ID)
                .from(MATCHES)
                .where(MATCHES.HOME_CLUB_ID.eq(homeClubId))
                .and(MATCHES.AWAY_CLUB_ID.eq(awayClubId))
                .and(MATCHES.MATCH_TYPE.eq(matchType))
                .and(MATCHES.SCHEDULED_DATE.greaterOrEqual(dayStart))
                .and(MATCHES.SCHEDULED_DATE.lt(nextDayStart))
                .fetchOneInto(Long.class);

        if (existingMatchId != null) {
            dsl.update(MATCHES)
                    .set(MATCHES.STATUS, status)
                    .set(MATCHES.SCHEDULED_DATE, scheduledDate)
                    .set(MATCHES.LOCATION_ID, locationId)
                    .where(MATCHES.ID.eq(existingMatchId))
                    .execute();
            return;
        }

        dsl.insertInto(MATCHES)
                .set(MATCHES.HOME_CLUB_ID, homeClubId)
                .set(MATCHES.AWAY_CLUB_ID, awayClubId)
                .set(MATCHES.MATCH_TYPE, matchType)
                .set(MATCHES.STATUS, status)
                .set(MATCHES.SCHEDULED_DATE, scheduledDate)
                .set(MATCHES.LOCATION_ID, locationId)
                .set(MATCHES.CREATED_AT, LocalDateTime.now())
                .execute();
    }

    private void seedAvailability(Long clubId, LocalDate date, String notes) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_SCHEDULES)
                        .where(CLUB_SCHEDULES.CLUB_ID.eq(clubId))
                        .and(CLUB_SCHEDULES.DATE.eq(date))
        );

        if (exists) {
            return;
        }

        dsl.insertInto(CLUB_SCHEDULES)
                .set(CLUB_SCHEDULES.CLUB_ID, clubId)
                .set(CLUB_SCHEDULES.DATE, date)
                .set(CLUB_SCHEDULES.STATUS, "FREE")
                .set(CLUB_SCHEDULES.NOTES, notes)
                .execute();
    }

    private void updateClubMedia(Long clubId, String logoUrl, String bannerUrl) {
        dsl.update(CLUBS)
                .set(CLUBS.LOGO_URL, logoUrl)
                .set(CLUBS.BANNER_URL, bannerUrl)
                .where(CLUBS.ID.eq(clubId))
                .execute();
    }

    private void updateClubContacts(Long clubId,
                                    String contactEmail,
                                    String whatsappNumber,
                                    String facebookMessengerUrl,
                                    ClubCommunicationMethod preferredCommunicationMethod) {
        dsl.update(CLUBS)
                .set(CLUBS.CONTACT_EMAIL, contactEmail)
                .set(CLUBS.WHATSAPP_NUMBER, whatsappNumber)
                .set(ClubDynamicTables.CLUBS_FACEBOOK_MESSENGER_URL, facebookMessengerUrl)
                .set(ClubDynamicTables.CLUBS_PREFERRED_CONTACT_METHOD, preferredCommunicationMethod.name())
                .where(CLUBS.ID.eq(clubId))
                .execute();
    }

    private void updateUserPresentation(Long userId, String profilePictureUrl) {
        dsl.update(USER_PROFILES)
                .set(USER_PROFILES.PROFILE_PICTURE_URL, profilePictureUrl)
                .where(USER_PROFILES.USER_ID.eq(userId))
                .execute();
    }

    private void upsertPlayerDetails(Long userId, String position, String preferredFoot, int heightCm, int weightKg, String availabilityStatus) {
        dsl.insertInto(PLAYER_DETAILS)
                .set(PLAYER_DETAILS.USER_ID, userId)
                .set(PLAYER_DETAILS.PRIMARY_POSITION, position)
                .set(PLAYER_DETAILS.PREFERRED_FOOT, preferredFoot)
                .set(PLAYER_DETAILS.HEIGHT_CM, heightCm)
                .set(PLAYER_DETAILS.WEIGHT_KG, weightKg)
                .set(PLAYER_DETAILS.AVAILABILITY_STATUS, availabilityStatus)
                .onDuplicateKeyUpdate()
                .set(PLAYER_DETAILS.PRIMARY_POSITION, position)
                .set(PLAYER_DETAILS.PREFERRED_FOOT, preferredFoot)
                .set(PLAYER_DETAILS.HEIGHT_CM, heightCm)
                .set(PLAYER_DETAILS.WEIGHT_KG, weightKg)
                .set(PLAYER_DETAILS.AVAILABILITY_STATUS, availabilityStatus)
                .execute();
    }

    private void attachMediaToPost(Long postId, String mediaUrl, Long uploadedBy) {
        Long mediaId = dsl.select(MEDIA.ID)
                .from(MEDIA)
                .where(MEDIA.URL.eq(mediaUrl))
                .fetchOneInto(Long.class);

        if (mediaId == null) {
            mediaId = dsl.insertInto(MEDIA)
                    .set(MEDIA.UPLOADED_BY, uploadedBy)
                    .set(MEDIA.URL, mediaUrl)
                    .set(MEDIA.TYPE, "image/svg+xml")
                    .set(MEDIA.SIZE_BYTES, 1024L)
                    .set(MEDIA.CREATED_AT, LocalDateTime.now())
                    .returningResult(MEDIA.ID)
                    .fetchOneInto(Long.class);
        }

        boolean alreadyLinked = dsl.fetchExists(
                dsl.selectOne()
                        .from(POST_MEDIA)
                        .where(POST_MEDIA.POST_ID.eq(postId))
                        .and(POST_MEDIA.MEDIA_ID.eq(mediaId))
        );

        if (alreadyLinked) {
            return;
        }

        Integer nextDisplayOrder = dsl.selectCount()
                .from(POST_MEDIA)
                .where(POST_MEDIA.POST_ID.eq(postId))
                .fetchOne(0, Integer.class);

        dsl.insertInto(POST_MEDIA)
                .set(POST_MEDIA.POST_ID, postId)
                .set(POST_MEDIA.MEDIA_ID, mediaId)
                .set(POST_MEDIA.DISPLAY_ORDER, nextDisplayOrder == null ? 0 : nextDisplayOrder)
                .execute();
    }

    private void ensureMembership(Long clubId, Long userId, String role) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_MEMBERSHIPS)
                        .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(userId))
        );

        if (exists) {
            return;
        }

        dsl.insertInto(CLUB_MEMBERSHIPS)
                .set(CLUB_MEMBERSHIPS.CLUB_ID, clubId)
                .set(CLUB_MEMBERSHIPS.USER_ID, userId)
                .set(CLUB_MEMBERSHIPS.ROLE, role)
                .set(CLUB_MEMBERSHIPS.JOINED_AT, LocalDateTime.now())
                .execute();
    }

    private void seedTrustLink(Long trustedClubId, Long trustingClubId, Long createdBy) {
        if (trustedClubId.equals(trustingClubId)) {
            return;
        }

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(ClubDynamicTables.CLUB_TRUST_LINKS)
                        .where(ClubDynamicTables.CLUB_TRUST_LINKS_TRUSTED_CLUB_ID.eq(trustedClubId))
                        .and(ClubDynamicTables.CLUB_TRUST_LINKS_TRUSTING_CLUB_ID.eq(trustingClubId))
        );

        if (exists) {
            return;
        }

        dsl.insertInto(ClubDynamicTables.CLUB_TRUST_LINKS)
                .set(ClubDynamicTables.CLUB_TRUST_LINKS_TRUSTED_CLUB_ID, trustedClubId)
                .set(ClubDynamicTables.CLUB_TRUST_LINKS_TRUSTING_CLUB_ID, trustingClubId)
                .set(ClubDynamicTables.CLUB_TRUST_LINKS_CREATED_BY, createdBy)
                .set(ClubDynamicTables.CLUB_TRUST_LINKS_CREATED_AT, LocalDateTime.now())
                .execute();
    }
}
