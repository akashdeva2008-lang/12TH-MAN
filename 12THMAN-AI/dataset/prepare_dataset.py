"""
12THMAN GPT-1 Cricket Dataset Generator & Preparator
Compiles comprehensive cricket rules, techniques, umpiring, formats, and QA pairs.
"""

import os
import json

DATASET_DIR = os.path.dirname(os.path.abspath(__file__))

CRICKET_CORPUS = [
    # 1. Cricket Rules & Fundamentals
    """Cricket is a bat-and-ball game played between two teams of eleven players on a cricket field at the centre of which is a 22-yard pitch with a wicket at each end, each comprising two bails balanced on three stumps. The batting side scores runs by striking the ball bowled at one of the wickets with the bat and then running between the wickets, while the bowling and fielding side tries to prevent this and dismiss each batter.
A match is divided into innings during which one team bats and the other bowls and fields. In limited-overs cricket, the match is played over a predetermined number of overs, typically 20 overs in T20 cricket or 50 overs in One Day Internationals (ODI).
Each over consists of six legal deliveries bowled by a single bowler from one end of the pitch. After an over is completed, the bowling end changes, and another bowler delivers the next over.""",

    # 2. Dismissals & Wickets
    """In cricket, a batsman can be dismissed in ten different ways according to the MCC Laws of Cricket:
1. Bowled: The bowler delivers the ball and it hits the stumps, dislodging the bails.
2. Caught: The batsman hits the ball with the bat or glove, and a fielder catches it before it touches the ground.
3. Leg Before Wicket (LBW): The ball strikes the batsman's pad or body in line with the stumps, without hitting the bat first, and would have hit the stumps in the umpire's judgment.
4. Run Out: A fielder breaks the wicket with the ball while the batsman is running and out of the crease.
5. Stumped: The wicket-keeper dislodges the bails while the batsman is out of the crease after attempting a shot and missing the ball.
6. Hit Wicket: The batsman dislodges their own bails with the bat, body, or equipment while playing a shot.
7. Handled the Ball / Obstructing the Field: The batsman willfully interferes with the ball or fielders.
8. Hit the Ball Twice: The batsman strikes the ball a second time except to guard the wicket.
9. Timed Out: An incoming batsman takes longer than the allowed time (typically 2 to 3 minutes) to arrive at the crease.
10. Retired Out: A batsman retires without the umpire's consent and cannot return to bat.""",

    # 3. Extras & Penalties
    """Extras are runs scored by methods other than the ball hitting the bat:
- Wide Ball (WD): Bowled too far away from the batsman to play a reasonable cricket stroke. Adds 1 run to the batting team, and the delivery must be re-bowled.
- No Ball (NB): Bowled illegally, such as the bowler overstepping the front popping crease, ball bouncing above head height, or bowling a waist-high full toss (beamer). Adds 1 run, must be re-bowled, and awards a Free Hit on the next delivery in limited-overs cricket.
- Bye (B): Runs scored when the ball passes the batsman without touching the bat or body, and the batsmen run safely between wickets.
- Leg Bye (LB): Runs scored when the ball deflects off the batsman's body or protective equipment (other than glove holding bat), provided the batsman attempted to play a stroke or avoid injury.""",

    # 4. Batting Techniques & Terminology
    """Batting in cricket involves a wide range of defensive and attacking strokes:
- Forward Defensive: Stepping forward to block the ball with a straight vertical bat towards the ground.
- Cover Drive: An elegant front-foot stroke played through the cover region against a full-pitched delivery outside off-stump.
- Straight Drive: Hitting the ball straight back past the bowler along the ground towards the boundary.
- Pull Shot: A cross-bat attacking stroke against a short-pitched delivery, hitting the ball to mid-wicket or square leg.
- Hook Shot: Playing a short delivery aimed around head height, hitting it backward of square leg.
- Cut Shot: A back-foot stroke using a horizontal bat to hit a short, wide delivery through point or gully.
- Sweep Shot: Played on one knee against spin bowling, sweeping the ball to fine leg or square leg.
- Upper Cut: Tilting the bat to guide a fast, short bouncer over the slips towards third man.
- Reverse Sweep: Switching the grip and stance to sweep the ball towards point against spin bowling.""",

    # 5. Bowling Techniques & Variations
    """Cricket bowling is divided into Pace (Fast) Bowling and Spin Bowling:
- Outswinger: A delivery that curves in the air away from the right-handed batsman, aiming for the outside edge.
- Inswinger: A delivery that curves in the air into the right-handed batsman, attacking the stumps and pads for LBW or Bowled.
- Yorker: A delivery pitched directly at the batsman's popping crease and toes, making it extremely difficult to hit.
- Bouncer: A fast, short-pitched delivery aimed at the batsman's chest or head level to force a mistake.
- Slower Ball: Disguised delivery released with less pace (such as back-of-the-hand or off-cutter) to deceive the batsman's timing.
- Off Break (Off Spin): Right-arm spin bowling that turns from the off-side into the right-handed batsman.
- Leg Break (Leg Spin): Right-arm spin bowling that turns from leg-side towards off-side away from the right-handed batsman.
- Googly: A leg-spinner's delivery that turns in the opposite direction, spinning into the right-handed batsman.
- Doosra: An off-spinner's delivery that turns away from the right-handed batsman like a leg break.""",

    # 6. Fielding Positions & Strategy
    """Fielding positions are strategically arranged across the cricket ground:
- Slips: Positioned behind the batsman on the off-side to catch edges (First Slip, Second Slip, Third Slip).
- Gully: Situated squarer than the slips on the off-side to catch thick edges.
- Point: Infield position square of the wicket on the off-side.
- Cover: Infield position between point and mid-off, responsible for saving singles and stopping cover drives.
- Mid-off: Infield position straight on the off-side near the bowler's end.
- Mid-on: Infield position straight on the on-side (leg-side) near the bowler's end.
- Square Leg: Infield position square of the wicket on the leg-side.
- Fine Leg: Boundary or ring position behind square on the leg-side.
- Third Man: Deep position behind square on the off-side.
- Long-on & Long-off: Deep boundary positions straight down the ground to prevent sixes.""",

    # 7. Cricket Formats & Tournament Rules
    """Modern cricket is played across several international and domestic formats:
1. T20 (Twenty20): Fast-paced 20-overs-per-side match lasting approximately 3.5 hours. Features a 6-over powerplay and maximum 4 overs per bowler.
2. One Day International (ODI): 50-overs-per-side match with powerplays (Overs 1-10 mandatory, 11-40 bowling powerplay, 41-50 batting powerplay). Maximum 10 overs per bowler.
3. Test Cricket: Traditional format played over 5 days with 90 overs per day. Each team bats two innings in red-ball (or pink-ball day-night) cricket with no over limits for bowlers.
4. Box Cricket: Casual and indoor format played on synthetic turf with 6 to 8 players, 6 to 10 overs, wall-rebound rules, and specific boundary zones.
5. The Hundred: 100-ball per side format with 5 or 10 ball ends, popularized in English franchise cricket.""",

    # 8. Scorecard & Mathematical Indicators
    """Understanding cricket statistics and scorecard metrics:
- Run Rate (CRR): Calculated as (Total Runs Scored) / (Total Overs Bowled). For example, 150 runs in 20 overs equals a run rate of 7.50.
- Required Run Rate (RRR): Calculated as (Remaining Runs Needed) / (Remaining Overs Left). Crucial in chasing scenarios during the second innings.
- Bowling Economy Rate: (Runs Conceded by Bowler) / (Overs Bowled). An economy below 7.0 in T20 is considered excellent.
- Batting Strike Rate: (Runs Scored * 100) / (Balls Faced). A strike rate above 140 indicates an aggressive power hitter.
- Batting Average: (Total Career Runs) / (Times Dismissed). An average over 40 in limited overs and over 50 in Test cricket is elite.
- Bowling Average: (Runs Conceded) / (Wickets Taken). Lower bowling average represents higher wicket-taking efficiency."""
]

# Cricket Q&A Pairs for Instruction Tuning & Chat
CRICKET_QA_DATA = [
    {
        "user": "What is an LBW in cricket?",
        "assistant": "LBW stands for Leg Before Wicket. A batsman can be given out LBW if the ball pitched in line or outside off-stump, strikes the batsman's pad without touching the bat first, and in the umpire's judgment would have gone on to hit the stumps. A batsman cannot be given LBW if the ball pitches outside the leg stump."
    },
    {
        "user": "What is the difference between an inswinger and an outswinger?",
        "assistant": "An inswinger is a pace delivery that moves through the air towards the right-handed batsman, aiming for the pads and stumps. An outswinger moves in the air away from the right-handed batsman, designed to tempt a drive and produce an outside edge caught by the wicketkeeper or slip cordon."
    },
    {
        "user": "How is bowling economy rate calculated?",
        "assistant": "Bowling Economy Rate is calculated by dividing the total runs conceded by the bowler by the number of overs bowled: Economy = Runs Conceded / Overs. For example, if a bowler concedes 24 runs in 4.0 overs, their economy rate is 6.00 runs per over."
    },
    {
        "user": "What happens on a No Ball in T20 cricket?",
        "assistant": "On a No Ball in T20 cricket, the batting team receives 1 penalty run, the delivery must be re-bowled, and the batsman cannot be dismissed on the no ball (except by run out, obstructing the field, or hitting the ball twice). Furthermore, the following legal delivery is designated as a Free Hit, where the batsman cannot be dismissed by standard dismissal modes."
    },
    {
        "user": "What is a Powerplay in T20 cricket?",
        "assistant": "In standard 20-over T20 cricket, the first 6 overs constitute the mandatory Powerplay. During these initial 6 overs, only 2 fielders are permitted outside the 30-yard circle, encouraging batsmen to play aerial attacking shots to exploit the field restrictions."
    },
    {
        "user": "What is a Googly?",
        "assistant": "A googly is a deceptive delivery bowled by a right-arm leg-spin bowler. While it appears identical to a standard leg-break in run-up and release, it spins in the opposite direction off the pitch—turning into the right-handed batsman from off to leg rather than turning away."
    },
    {
        "user": "What is a Yorker and why is it effective?",
        "assistant": "A yorker is a delivery pitched right at the batsman's popping crease, targeting their toes. It is highly effective, especially in death overs, because it leaves no room for the batsman to get under the ball for lofted shots, often resulting in bowled or LBW dismissals."
    },
    {
        "user": "Explain the Duckworth-Lewis-Stern (DLS) method.",
        "assistant": "The Duckworth-Lewis-Stern (DLS) method is a mathematical formulation used in rain-interrupted limited-overs cricket matches to recalculate target scores. It accounts for two remaining resources: overs left and wickets in hand, ensuring a fair revised target for the chasing team."
    },
    {
        "user": "What are the key responsibilities of a 12th man in cricket?",
        "assistant": "The 12th man is the primary substitute player in a cricket team squad. While they cannot bat, bowl, or act as captain, they take the field as a substitute fielder when an active player is injured, deliver hydration and tactical messages from coaching staff, and assist with equipment management."
    },
    {
        "user": "How does strike rotation work when scoring odd runs?",
        "assistant": "When batsmen score an odd number of runs (1 or 3 runs), they cross an odd number of times, causing the striker and non-striker to swap ends. The non-striker becomes the new striker for the next delivery. At the end of an over (after 6 legal deliveries), strike rotates again to begin the new over from the opposite bowling end."
    },
    {
        "user": "What is the difference between a Bye and a Leg Bye?",
        "assistant": "A Bye occurs when the ball passes the batsman without contacting either the bat or the batsman's body, and the batsmen run safely. A Leg Bye occurs when the ball deflects off the batsman's body or protective equipment (other than the gloves holding the bat), provided the batsman either attempted to hit the ball or tried to avoid injury."
    },
    {
        "user": "What is a Mankad dismissal in cricket?",
        "assistant": "A Mankad, formally known as 'Run Out at Non-Striker's End' under Law 38, occurs when a bowler removes the bails at the non-striker's end before releasing the ball because the non-striker has backed up out of their crease prematurely. It is an official and legitimate mode of dismissal."
    },
    {
        "user": "What is batting strike rate?",
        "assistant": "Batting strike rate measures a batsman's scoring speed, calculated as: (Runs Scored * 100) / Balls Faced. A strike rate of 150.0 means the batsman scores an average of 1.5 runs per ball faced."
    },
    {
        "user": "What is a Super Over?",
        "assistant": "A Super Over is a tie-breaker procedure used in limited-overs cricket when both teams finish with equal scores. Each team bats for one over (6 legal balls) with up to two wickets (three batsmen). The team that scores more runs in the Super Over wins the match."
    },
    {
        "user": "What are Box Cricket rules?",
        "assistant": "Box Cricket is played on an enclosed turf pitch with 6 to 8 players per side across 6 to 10 overs. Key rules often include: direct boundary hits out of the box are declared out, wall rebounds are in play for runs, and underarm or restricted bowling actions may be enforced."
    }
]


def generate_dataset():
    """
    Generate dataset files:
    1. cricket_knowledge.txt (pretraining corpus)
    2. cricket_qa.jsonl (instruction / conversation pairs)
    """
    os.makedirs(DATASET_DIR, exist_ok=True)

    # 1. Generate Raw Corpus TXT
    raw_corpus_path = os.path.join(DATASET_DIR, "cricket_knowledge.txt")
    with open(raw_corpus_path, "w", encoding="utf-8") as f:
        # Write knowledge articles
        for article in CRICKET_CORPUS:
            f.write(article.strip() + "\n\n")

        # Write Q&A formatted texts
        for qa in CRICKET_QA_DATA:
            f.write(f"Question: {qa['user']}\nAnswer: {qa['assistant']}\n\n")

    # 2. Generate JSONL Q&A Dataset
    jsonl_path = os.path.join(DATASET_DIR, "cricket_qa.jsonl")
    with open(jsonl_path, "w", encoding="utf-8") as f:
        for qa in CRICKET_QA_DATA:
            f.write(json.dumps(qa, ensure_ascii=False) + "\n")

    # 3. Document Dataset Metadata
    metadata_path = os.path.join(DATASET_DIR, "dataset_info.json")
    metadata = {
        "dataset_name": "12THMAN Cricket Domain Corpus",
        "version": "1.0",
        "description": "Comprehensive cricket rules, tactics, formats, dismissals, and terminology created for training 12THMAN GPT-1 from scratch.",
        "num_articles": len(CRICKET_CORPUS),
        "num_qa_pairs": len(CRICKET_QA_DATA),
        "license": "Custom Proprietary for 12THMAN Cricket Platform",
        "topics_covered": [
            "Cricket Rules & MCC Laws",
            "Dismissal Modes (LBW, Bowled, Run Out, Stumped)",
            "Batting Strokes (Cover Drive, Pull, Hook, Sweep)",
            "Bowling Techniques (Pace, Swing, Spin, Yorker, Bouncer)",
            "Fielding Positions (Slips, Gully, Point, Cover)",
            "Extras & Free Hit Rules (Wides, No-Balls, Byes)",
            "Tournament Formats (T20, ODI, Test, Box Cricket)",
            "Scorecard Statistics (Run Rate, Economy, Strike Rate, Average)"
        ]
    }
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print(f"[Dataset] Generated {raw_corpus_path} and {jsonl_path}")


if __name__ == "__main__":
    generate_dataset()
