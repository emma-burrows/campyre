package campyre.java;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Message {
	// Campfire message types
	public static enum Type {
	  // Campfire message types
	  TEXT, IMAGE, TIMESTAMP, ENTRY, LEAVE, PASTE, TOPIC,
	  // Special message types
	  UNSUPPORTED,
	  /** can be used by clients to make messages that didn't come from the Campfire. */
	  ERROR,
	  /** can be used by clients to make messages that are still in transit */
	  TRANSIT;

	  private static Type typeFor(String type, String body) {
	    if (type.equals("TextMessage")) {
	      if (imageLink(body))
	        return IMAGE;
	      else
	        return TEXT;
	    } else if (type.equals("PasteMessage"))
	      return PASTE;
	    else if (type.equals("TimestampMessage"))
	      return TIMESTAMP;
	    else if (type.equals("EnterMessage"))
	      return ENTRY;
	    else if (type.equals("LeaveMessage") || type.equals("KickMessage"))
	      return LEAVE;
	    else if (type.equals("TopicChangeMessage"))
	      return TOPIC;
	    else
	      return UNSUPPORTED;
	  }
	}

	public static boolean loadImages;

	public Type type;
	public String id, user_id, body;
	public Date timestamp;

	private String[] inFormat = new String[] {"yy/MM/dd HH:mm:ss Z"};

	// Here for the Android client, the display name to put on the Message object itself
	public String person;

	// for making artificial messages (really just intended to serve the Android client)
	// only make them if you know what you're doing (as they'll be missing fields!)
	public Message(String id, Type type, String body) {
		this.id = id;
		this.type = type;
		this.body = body;
	}

	public Message(JSONObject json) throws JSONException, DateParseException {
		String body = denull(replaceEmoji(json.getString("body")));

		this.body = body;
		this.type = Type.typeFor(json.getString("type"), body);

		this.id = json.getString("id");
		this.user_id = denull(json.getString("user_id"));
		this.timestamp = DateUtils.parseDate(json.getString("created_at"), inFormat);
		this.person = null;
	}

	public static ArrayList<Message> allToday(Room room) throws CampfireException {
		ArrayList<Message> messages = new ArrayList<Message>();

		try {
			JSONArray items = new CampfireRequest(room.campfire).getList(todayPath(room.id), "messages");
			int length = items.length();

			for (int i=0; i<length; i++) {
				Message message = new Message(items.getJSONObject(i));
				if (message.type != Type.UNSUPPORTED)
					messages.add(message);
			}

		} catch (JSONException e) {
			throw new CampfireException(e, "Could not load messages from their JSON.");
		} catch (DateParseException e) {
			throw new CampfireException(e, "Could not parse date from a message's JSON.");
		}

		return messages;
	}

	public static ArrayList<Message> recent(Room room, int max, String lastSeen) throws CampfireException {
		ArrayList<Message> messages = new ArrayList<Message>();

		try {
			HashMap<String,String> parameters = new HashMap<String,String>();
			parameters.put("limit", String.valueOf(max));
			if (lastSeen != null)
				parameters.put("since_message_id", lastSeen);

			JSONArray items = new CampfireRequest(room.campfire).getList(recentPath(room.id), parameters, "messages");
			int length = items.length();
			for (int i=0; i<length; i++) {
				Message message = new Message(items.getJSONObject(i));
				if (message.type != Type.UNSUPPORTED)
					messages.add(message);
			}

		} catch (JSONException e) {
			throw new CampfireException(e, "Could not load messages from their JSON.");
		} catch (DateParseException e) {
			throw new CampfireException(e, "Could not parse date from a message's JSON.");
		}

		return messages;
	}

	public static String recentPath(String room_id) {
		return "/room/" + room_id + "/recent";
	}

	public static String todayPath(String room_id) {
		return "/room/" + room_id + "/transcript";
	}

	private String denull(String maybeNull) {
		if (maybeNull.equals("null"))
			return null;
		else
			return maybeNull;
	}

	// depends on the assumption that we'll only render image links that are the entirety of the body
	// if we ever expand this assumption, this will need to also extract the URL
	public static boolean imageLink(String body) {
		if (!loadImages) {
			return false;
		}
		Pattern pattern = Pattern.compile("^(http[^\\s]+(?:jpe?g|gif|png))(\\?[^\\s]*)?$");
		Matcher matcher = pattern.matcher(body);
		return matcher.matches();
	}
	
	public String replaceEmoji(String body) {
		if (!loadImages) {
			return body;
		}
		Pattern pattern = Pattern.compile(":.+?:");
		Matcher matcher = pattern.matcher(body);
		String finalBody = body;
		if(matcher.matches()) {
			String emoji = getEmoji(":smile:");
			finalBody = body.replaceAll(":.+?:", emoji);
		}
		return finalBody;
	}
	
	private String getEmoji(String key) {
		HashMap<String, Character> emoji = new HashMap<String, Character>();
		
		emoji.put(":smile:", '\u2600');
		
		return emoji.get(key).toString();
		
/*				{               @, 
            @":blush:", @"\ue056", 
            @":smiley:", @"\ue057", 
            @":relaxed:", @"\ue414", 
            @":smirk:", @"\ue402", 
            @":heart_eyes:", @"\ue106", 
            @":kissing_heart:", @"\ue418", 
            @":kissing_face:", @"\ue417", 
            @":flushed:", @"\ue40d", 
            @":relieved:", @"\ue401", 
            @":satisfied:", @"\ue40a", 
            @":grin:", @"\ue404", 
            @":wink:", @"\ue405", 
            @":wink2:", @"\ue105", 
            @":tongue:", @"\ue409", 
            @":unamused:", @"\ue40e", 
            @":sweat:", @"\ue108", 
            @":pensive:", @"\ue403", 
            @":disappointed:", @"\ue058", 
            @":confounded:", @"\ue407", 
            @":fearful:", @"\ue40b", 
            @":cold_sweat:", @"\ue40f", 
            @":persevere:", @"\ue406", 
            @":cry:", @"\ue413", 
            @":sob:", @"\ue411", 
            @":joy:", @"\ue412", 
            @":astonished:", @"\ue410", 
            @":scream:", @"\ue107", 
            @":angry:", @"\ue059", 
            @":rage:", @"\ue416", 
            @":sleepy:", @"\ue408", 
            @":mask:", @"\ue40c", 
            @":imp:", @"\ue11a", 
            @":alien:", @"\ue10c", 
            @":yellow_heart:", @"\ue32c", 
            @":blue_heart:", @"\ue32a", 
            @":purple_heart:", @"\ue32d", 
            @":heart:", @"\ue022", 
            @":green_heart:", @"\ue32b", 
            @":broken_heart:", @"\ue023", 
            @":heartbeat:", @"\ue327", 
            @":heartpulse:", @"\ue328",
            @":cupid:", @"\ue329", 
            @":sparkles:", @"\ue32e", 
            @":star:", @"\ue335", // add star2
            //@":star2:", @"", // can't find different star in iOS emoji keyboard
            @":anger:", @"\ue334", 
            @":exclamation:", @"\ue337", // add grey_exclamation
            @":question:", @"\ue336", // add grey_question
            // @":grey_exclamation:", @"❕", // can't find in iOS emoji keyboard
            // @":grey_question:", @"❔", can't find in iOS emoji keyboard
            @":zzz:", @"\ue13c", 
            @":dash:", @"\ue330", 
            @":sweat_drops:", @"\ue331", 
            @":notes:", @"\ue326", 
            @":musical_note:", @"\ue03e", 
            @":fire:", @"\ue11d", 
            @":hankey:", @"\ue05a", // add poop, shit
            // @":poop:", @"", 
            // @":shit:", @"", 
            @":+1:", @"\ue00e", // add thumbsup
            // @":thumbsup:", @"", 
            @":-1:", @"\ue421", // add thumbsdown
            // @":thumbsdown:", @"", 
            @":ok_hand:", @"\ue420", 
            @":punch:", @"\ue00d", 
            @":fist:", @"\ue010", 
            @":v:", @"\ue011", 
            @":wave:", @"\ue41e", 
            @":hand:", @"\ue012", 
            @":open_hands:", @"\ue422", 
            @":point_up:", @"\ue22e", 
            @":point_down:", @"\ue22f", 
            @":point_left:", @"\ue230", 
            @":point_right:", @"\ue231", 
            @":raised_hands:", @"\ue427", 
            @":pray:", @"\ue41d", 
            @":point_up_2:", @"\ue00f", 
            @":clap:", @"\ue41f", 
            @":muscle:", @"\ue14c", 
            //@":metal:", @"", // can't find in iOS emoji keyboard
            @":walking:", @"\ue201", 
            @":runner:", @"\ue115", 
            @":couple:", @"\ue428", 
            @":dancer:", @"\ue51f", 
            @":dancers:", @"\ue429", 
            @":ok_woman:", @"\ue424", 
            @":no_good:", @"\ue423", 
            @":information_desk_person:", @"\ue253", 
            @":bow:", @"\ue426", 
            @":couplekiss:", @"\ue111", 
            @":couple_with_heart:", @"\ue425", 
            @":massage:", @"\ue31e", 
            @":haircut:", @"\ue31f", 
            @":nail_care:", @"\ue31d", 
            @":boy:", @"\ue001", 
            @":girl:", @"\ue002", 
            @":woman:", @"\ue005", 
            @":man:", @"\ue004", 
            @":baby:", @"\ue51a", 
            @":older_woman:", @"\ue519", 
            @":older_man:", @"\ue518", 
            @":person_with_blond_hair:", @"\ue515", 
            @":man_with_gua_pi_mao:", @"\ue516", 
            @":man_with_turban:", @"\ue517", 
            @":construction_worker:", @"\ue51b", 
            @":cop:", @"\ue152", 
            @":angel:", @"\ue04e", 
            @":princess:", @"\ue51c", 
            @":guardsman:", @"\ue51e", 
            @":skull:", @"\ue11c", 
            @":feet:", @"\ue536", 
            @":lips:", @"\ue41c", 
            @":kiss:", @"\ue003", 
            @":ear:", @"\ue41b", 
            @":eyes:", @"\ue419", 
            @":nose:", @"\ue41a", 
            // @":feelsgood:", @"", // can't find any of these in iOS emoji keyboard:
            // @":finnadie:", @"", 
            // @":goberserk:", @"", 
            // @":godmode:", @"", 
            // @":hurtrealbad:", @"", 
            // @":rage1:", @"", 
            // @":rage2:", @"", 
            // @":rage3:", @"", 
            // @":rage4:", @"", 
            // @":suspect:", @"", 
            
            // Nature
            @":sunny:", @"\ue04a", 
            @":umbrella:", @"\ue04b", 
            @":cloud:", @"\ue049", 
            @":snowman:", @"\ue048", 
            @":moon:", @"\ue04c", 
            @":zap:", @"\ue13d", 
            @":cyclone:", @"\ue443", 
            @":ocean:", @"\ue43e", 
            @":cat:", @"\ue04f", 
            @":dog:", @"\ue052", 
            @":mouse:", @"\ue053", 
            @":hamster:", @"\ue524", 
            @":rabbit:", @"\ue52c", 
            @":wolf:", @"\ue52a", 
            @":frog:", @"\ue531", 
            @":tiger:", @"\ue050", 
            @":koala:", @"\ue527", 
            @":bear:", @"\ue051", 
            @":pig:", @"\ue10b", 
            @":cow:", @"\ue52b", 
            @":boar:", @"\ue52f", 
            @":monkey_face:", @"\ue109", 
            @":monkey:", @"\ue528", 
            @":horse:", @"\ue01a", 
            @":racehorse:", @"\ue134", 
            @":camel:", @"\ue530", 
            @":sheep:", @"\ue529", 
            @":elephant:", @"\ue526", 
            @":snake:", @"\ue52d", 
            @":bird:", @"\ue521", 
            @":baby_chick:", @"\ue523", 
            @":chicken:", @"\ue52e", 
            @":penguin:", @"\ue055", 
            @":bug:", @"\ue525", 
            @":octopus:", @"\ue10a", 
            @":tropical_fish:", @"\ue522", 
            @":fish:", @"\ue019", 
            @":whale:", @"\ue054", 
            @":dolphin:", @"\ue520", 
            @":bouquet:", @"\ue306", 
            @":cherry_blossom:", @"\ue030", 
            @":tulip:", @"\ue304", 
            @":four_leaf_clover:", @"\ue110", 
            @":rose:", @"\ue032", 
            @":sunflower:", @"\ue305", 
            @":hibiscus:", @"\ue303", 
            @":maple_leaf:", @"\ue118", 
            @":leaves:", @"\ue447", 
            @":fallen_leaf:", @"\ue119", 
            @":palm_tree:", @"\ue307", 
            @":cactus:", @"\ue308", 
            @":ear_of_rice:", @"\ue444", 
            @":shell:", @"\ue441", 
            // @":octocat:", @"", // not found either of these in iOS emoji keyboard
            // @":squirrel:", @"", 
            
            // Objects
            @":bamboo:", @"\ue436", 
            @":gift_heart:", @"\ue437", 
            @":dolls:", @"\ue438", 
            @":school_satchel:", @"\ue43a", 
            @":mortar_board:", @"\ue439", 
            @":flags:", @"\ue43b", 
            @":fireworks:", @"\ue117", 
            @":sparkler:", @"\ue440", 
            @":wind_chime:", @"\ue442", 
            @":rice_scene:", @"\ue446", 
            @":jack_o_lantern:", @"\ue445", 
            @":ghost:", @"\ue11b", 
            @":santa:", @"\ue448", 
            @":christmas_tree:", @"\ue033", 
            @":gift:", @"\ue112", 
            @":bell:", @"\ue325", 
            @":tada:", @"\ue312", 
            @":balloon:", @"\ue310", 
            @":cd:", @"\ue126", 
            @":dvd:", @"\ue127", 
            @":camera:", @"\ue008", 
            @":movie_camera:", @"\ue03d", 
            @":computer:", @"\ue00c", 
            @":tv:", @"\ue12a", 
            @":iphone:", @"\ue00a", 
            @":fax:", @"\ue00b", 
            @":phone:", @"\ue009", // add telephone
            // @":telephone:", @"☎", 
            @":minidisc:", @"\ue316", 
            @":vhs:", @"\ue129", 
            @":speaker:", @"\ue141", 
            @":loudspeaker:", @"\ue142", 
            @":mega:", @"\ue317", 
            @":radio:", @"\ue128", 
            @":satellite:", @"\ue14b", 
            @":loop:", @"\ue211", 
            @":mag:", @"\ue114", 
            @":unlock:", @"\ue145", 
            @":lock:", @"\ue144", 
            @":key:", @"\ue03f", 
            @":scissors:", @"\ue313", 
            @":hammer:", @"\ue116", 
            @":bulb:", @"\ue10f",
            @":calling:", @"\ue104", 
            @":email:", @"\ue103", 
            @":mailbox:", @"\ue101", 
            @":postbox:", @"\ue102", 
            @":bath:", @"\ue13f", 
            @":toilet:", @"\ue140", 
            @":seat:", @"\ue11f", 
            @":moneybag:", @"\ue12f", 
            @":trident:", @"\ue031", 
            @":smoking:", @"\ue30e", 
            @":bomb:", @"\ue311", 
            @":gun:", @"\ue113", 
            @":pill:", @"\ue30f", 
            @":syringe:", @"\ue13b", 
            @":football:", @"\ue42b", 
            @":basketball:", @"\ue42a", 
            @":soccer:", @"\ue018", 
            @":baseball:", @"\ue016", 
            @":tennis:", @"\ue015", 
            @":golf:", @"\ue014", 
            @":8ball:", @"\ue42c", 
            @":swimmer:", @"\ue42d", 
            @":surfer:", @"\ue017", 
            @":ski:", @"\ue013", 
            @":spades:", @"\ue20e", 
            @":hearts:", @"\ue20c", 
            @":clubs:", @"\ue20f", 
            @":diamonds:", @"\ue20d", 
            @":gem:", @"\ue035", 
            @":ring:", @"\ue034", 
            @":trophy:", @"\ue131", 
            @":space_invader:", @"\ue12b", 
            @":dart:", @"\ue130", 
            @":mahjong:", @"\ue12d", 
            @":clapper:", @"\ue324", 
            @":memo:", @"\ue301", // add pencil
            // @":pencil:", @"", 
            @":book:", @"\ue148", 
            @":art:", @"\ue502", 
            @":microphone:", @"\ue03c", 
            @":headphones:", @"\ue30a", 
            @":trumpet:", @"\ue042", 
            @":saxophone:", @"\ue040", 
            @":guitar:", @"\ue041", 
            @":part_alternation_mark:", @"\ue12c", 
            @":shoe:", @"\ue007", 
            @":sandal:", @"\ue31a", 
            @":high_heel:", @"\ue13e", 
            @":lipstick:", @"\ue31c", 
            @":boot:", @"\ue31b", 
            @":shirt:", @"\ue006", // add tshirt
            // @":tshirt:", @"", 
            @":necktie:", @"\ue302", 
            @":dress:", @"\ue319", 
            @":kimono:", @"\ue321", 
            @":bikini:", @"\ue322", 
            @":ribbon:", @"\ue314", 
            @":tophat:", @"\ue503", 
            @":crown:", @"\ue10e", 
            @":womans_hat:", @"\ue318", 
            @":closed_umbrella:", @"\ue43c", 
            @":briefcase:", @"\ue11e", 
            @":handbag:", @"\ue323", 
            @":beer:", @"\ue047", 
            @":beers:", @"\ue30c", 
            @":cocktail:", @"\ue044", 
            @":sake:", @"\ue30b", 
            @":fork_and_knife:", @"\ue043", 
            @":hamburger:", @"\ue120", 
            @":fries:", @"\ue33b", 
            @":spaghetti:", @"\ue33f", 
            @":curry:", @"\ue341", 
            @":bento:", @"\ue34c", 
            @":sushi:", @"\ue344", 
            @":rice_ball:", @"\ue342", 
            @":rice_cracker:", @"\ue33d", 
            @":rice:", @"\ue33e", 
            @":ramen:", @"\ue340", 
            @":stew:", @"\ue34d", 
            @":bread:", @"\ue339", 
            @":egg:", @"\ue147", 
            @":oden:", @"\ue343", 
            @":dango:", @"\ue33c", 
            @":icecream:", @"\ue33a", 
            @":shaved_ice:", @"\ue43f", 
            @":birthday:", @"\ue34b", 
            @":cake:", @"\ue046", 
            @":apple:", @"\ue345", 
            @":tangerine:", @"\ue346", 
            @":watermelon:", @"\ue348", 
            @":strawberry:", @"\ue347", 
            @":eggplant:", @"\ue34a", 
            @":tomato:", @"\ue349", 
            @":coffee:", @"\ue045", 
            @":tea:", @"\ue338", 
            
            // Places
            
            @":109:", @"\ue50a", // symbol appears only on iOS 4 emoji keyboard (not 5+)
            @":house:", @"\ue036", 
            @":school:", @"\ue157", 
            @":office:", @"\ue038", 
            @":post_office:", @"\ue153", 
            @":hospital:", @"\ue155", 
            @":bank:", @"\ue14d", 
            @":convenience_store:", @"\ue156", 
            @":love_hotel:", @"\ue501", 
            @":hotel:", @"\ue158", 
            @":wedding:", @"\ue43d", 
            @":church:", @"\ue037", 
            @":department_store:", @"\ue504", 
            @":city_sunrise:", @"\ue44a", 
            @":city_sunset:", @"\ue146", 
            @":japanese_castle:", @"\ue505", 
            @":european_castle:", @"\ue506", 
            @":tent:", @"\ue122", 
            @":factory:", @"\ue508", 
            @":tokyo_tower:", @"\ue509", 
            @":mount_fuji:", @"\ue03b", 
            @":sunrise_over_mountains:", @"\ue04d", 
            @":sunrise:", @"\ue449", 
            @":stars:", @"\ue44b", 
            @":statue_of_liberty:", @"\ue51d", 
            @":rainbow:", @"\ue44c", 
            @":ferris_wheel:", @"\ue124", 
            @":fountain:", @"\ue121", 
            @":roller_coaster:", @"\ue433", 
            @":ship:", @"\ue202", 
            @":speedboat:", @"\ue135", 
            @":boat:", @"\ue01c", // add sailboat
            // @":sailboat:", @"⛵", 
            @":airplane:", @"\ue01d", 
            @":rocket:", @"\ue10d", 
            @":bike:", @"\ue136", 
            @":blue_car:", @"\ue42e", 
            @":car:", @"\ue01b", // add red_car
            // @":red_car:", @"", 
            @":taxi:", @"\ue15a", 
            @":bus:", @"\ue159", 
            @":police_car:", @"\ue432", 
            @":fire_engine:", @"\ue430", 
            @":ambulance:", @"\ue431", 
            @":truck:", @"\ue42f", 
            @":train:", @"\ue01e", 
            @":station:", @"\ue039", 
            @":bullettrain_front:", @"\ue01f", 
            @":bullettrain_side:", @"\ue435", 
            @":ticket:", @"\ue125", 
            @":fuelpump:", @"\ue03a", 
            @":traffic_light:", @"\ue14e", 
            @":warning:", @"\ue252", 
            @":construction:", @"\ue137", 
            @":beginner:", @"\ue209", 
            @":atm:", @"\ue154", 
            @":slot_machine:", @"\ue133", 
            @":busstop:", @"\ue150", 
            @":barber:", @"\ue320", 
            @":hotsprings:", @"\ue123", 
            @":checkered_flag:", @"\ue132", 
            @":crossed_flags:", @"\ue143", 
            @":jp:", @"\ue50b", 
            @":kr:", @"\ue514", 
            @":cn:", @"\ue513", 
            @":us:", @"\ue50c", 
            @":fr:", @"\ue50d", 
            @":es:", @"\ue511", 
            @":it:", @"\ue50f", 
            @":ru:", @"\ue512", 
            @":gb:", @"\ue510", 
            @":de:", @"\ue50e", 
            
            // Symbols
            
            @":1:", @"\ue21c", 
            @":2:", @"\ue21d", 
            @":3:", @"\ue21e", 
            @":4:", @"\ue21f", 
            @":5:", @"\ue220", 
            @":6:", @"\ue221", 
            @":7:", @"\ue222", 
            @":8:", @"\ue223", 
            @":9:", @"\ue224",
            @":0:", @"\ue225", 
            @":hash:", @"\ue210", 
            @":arrow_backward:", @"\ue23b", 
            @":arrow_down:", @"\ue233", 
            @":arrow_forward:", @"\ue23a", 
            @":arrow_left:", @"\ue235", 
            @":arrow_lower_left:", @"\ue239", 
            @":arrow_lower_right:", @"\ue238", 
            @":arrow_right:", @"\ue234", 
            @":arrow_up:", @"\ue232", 
            @":arrow_upper_left:", @"\ue237", 
            @":arrow_upper_right:", @"\ue236", 
            @":rewind:", @"\ue23d", 
            @":fast_forward:", @"\ue23c", 
            @":ok:", @"\ue24d", 
            @":new:", @"\ue212", 
            @":top:", @"\ue24c", 
            @":up:", @"\ue213", 
            @":cool:", @"\ue214", 
            @":cinema:", @"\ue507", 
            @":koko:", @"\ue203", 
            @":signal_strength:", @"\ue20b", 
            @":u5272:", @"\ue227", 
            @":u55b6:", @"\ue22d", 
            @":u6307:", @"\ue22c", 
            @":u6708:", @"\ue217", 
            @":u6709:", @"\ue215", 
            @":u6e80:", @"\ue22a", 
            @":u7121:", @"\ue216", 
            @":u7533:", @"\ue218", 
            @":u7a7a:", @"\ue22b", 
            @":sa:", @"\ue228", 
            @":restroom:", @"\ue151", 
            @":mens:", @"\ue138", 
            @":womens:", @"\ue139", 
            @":baby_symbol:", @"\ue13a", 
            @":no_smoking:", @"\ue208", 
            @":parking:", @"\ue14f", 
            @":wheelchair:", @"\ue20a", 
            @":metro:", @"\ue434", 
            @":wc:", @"\ue309", 
            @":secret:", @"\ue315", 
            @":congratulations:", @"\ue30d", 
            @":ideograph_advantage:", @"\ue226", // looks slightly different but kanji is the same
            @":underage:", @"\ue207", 
            @":id:", @"\ue229", 
            @":eight_spoked_asterisk:", @"\ue206", 
            @":eight_pointed_black_star:", @"\ue205", 
            @":heart_decoration:", @"\ue204", 
            @":vs:", @"\ue12e", 
            @":vibration_mode:", @"\ue250", 
            @":mobile_phone_off:", @"\ue251", 
            @":chart:", @"\ue14a", 
            @":currency_exchange:", @"\ue149", 
            @":aries:", @"\ue23f", 
            @":taurus:", @"\ue240", 
            @":gemini:", @"\ue241", 
            @":cancer:", @"\ue242", 
            @":leo:", @"\ue243", 
            @":virgo:", @"\ue244", 
            @":libra:", @"\ue245", 
            @":scorpius:", @"\ue246", 
            @":sagittarius:", @"\ue247", 
            @":capricorn:", @"\ue248", 
            @":aquarius:", @"\ue249", 
            @":pisces:", @"\ue24a", 
            @":ophiuchus:", @"\ue24b", 
            @":six_pointed_star:", @"\ue23e", 
            @":a:", @"\ue532", 
            @":b:", @"\ue533", 
            @":ab:", @"\ue534", 
            @":o2:", @"\ue535", 
            @":red_circle:", @"\ue219", 
            // @":black_square:", @"", // these show as green
            // @":white_square:", @"", // and purple circles on iOS 4 so leaving to come through as text
            @":clock1:", @"\ue024", 
            @":clock10:", @"\ue02d", 
            @":clock11:", @"\ue02e", 
            @":clock12:", @"\ue02f", 
            @":clock2:", @"\ue025", 
            @":clock3:", @"\ue026", 
            @":clock4:", @"\ue027", 
            @":clock5:", @"\ue028", 
            @":clock6:", @"\ue029", 
            @":clock7:", @"\ue02a", 
            @":clock8:", @"\ue02b", 
            @":clock9:", @"\ue02c", 
            @":o:", @"\ue332", 
            @":x:", @"\ue333", 
            @":copyright:", @"\u00a9", 
            @":registered:", @"\u00ae", 
            @":tm:", @"\u2122"}*/
	}
}