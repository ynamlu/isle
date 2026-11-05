package com.isle.companion.domain.scenario

/** 一个轻量的引导式场景，不是固定剧本，而是一条“共情 → 探索 → 小行动”的路径。 */
data class Scenario(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val opening: String,
    val goal: String,
    val steps: List<String>,
    val suggestedRole: String = "bestie"
)

object ScenarioCatalog {

    val all: List<Scenario> = listOf(
        Scenario(
            id = "exam_anxiety",
            title = "期末复习焦虑",
            subtitle = "脑子很乱，书却看不进去",
            emoji = "📚",
            opening = "我在。先不用逼自己马上进入状态——最近一想到考试，最让你紧张的是哪一门？",
            goal = "把“巨大的焦虑”拆成一个今天能完成的小动作。",
            steps = listOf(
                "先说出最焦虑的一门课和具体担心",
                "区分“事实”和“脑子里的最坏结果”",
                "一起选一个 20 分钟能开始的小任务",
                "约定今天只完成这一步，不要求全部做完"
            )
        ),
        Scenario(
            id = "interview_fail",
            title = "刚刚面试失败",
            subtitle = "觉得自己很差劲",
            emoji = "💼",
            opening = "面试失败确实很难受。先不急着总结教训，你愿意先说说，现在心里最刺的那句话是什么吗？",
            goal = "把“我不行”还原成一次具体事件，并找到下一次可调整的一点。",
            steps = listOf(
                "先命名情绪：失落、羞耻、愤怒还是不甘",
                "区分“这次没通过”和“我这个人不行”",
                "找一个客观原因和一个可改进点",
                "写下下一次可以提前准备的一件事"
            )
        ),
        Scenario(
            id = "dorm_conflict",
            title = "宿舍关系让我很累",
            subtitle = "想说又怕撕破脸",
            emoji = "🏠",
            opening = "宿舍里的摩擦很消耗人。你可以先告诉我，最近发生了什么，让你最不舒服的那个瞬间是什么？",
            goal = "把感受和需求说清楚，形成一个可执行的沟通句式。",
            steps = listOf(
                "描述具体事件，不给对方贴标签",
                "说出这件事带给你的感受",
                "明确你的需求或边界",
                "一起练一句不攻击人的表达"
            )
        ),
        Scenario(
            id = "family_pressure",
            title = "和家人吵架了",
            subtitle = "一说话就被否定",
            emoji = "🫂",
            opening = "和家人吵完，心里常常又气又委屈。你愿意先说说，最让你难受的是哪句话吗？",
            goal = "先安顿情绪，再区分“他们的期待”和“你真正想要的”。",
            steps = listOf(
                "还原争吵里最刺耳的一句话",
                "说出你当时的感受和需要",
                "区分对方的期待与你的选择",
                "确定一个暂时保护自己的边界"
            )
        ),
        Scenario(
            id = "insomnia",
            title = "最近总是睡不着",
            subtitle = "越想睡越清醒",
            emoji = "🌙",
            opening = "睡不着的时候，脑子是不是反而更吵？我们可以先不追求“立刻睡着”，先让身体慢下来一点。",
            goal = "降低睡前反刍，建立一个 10 分钟的睡前缓冲。",
            steps = listOf(
                "说说躺下后最常出现的念头",
                "做一轮慢呼吸或身体扫描",
                "把明天要做的事放到纸面/备忘录",
                "约定今晚只做一件让身体放松的事"
            )
        ),
        Scenario(
            id = "no_motivation",
            title = "什么都不想做",
            subtitle = "知道该做，但提不起劲",
            emoji = "🫧",
            opening = "提不起劲不代表你懒，可能是这段时间太满了。我们先不要求“振作”，只看看能不能挪动一小步。",
            goal = "用“五分钟启动”代替“必须做完”的压力。",
            steps = listOf(
                "说出最想逃避的那件事",
                "把任务缩到 5 分钟版本",
                "允许自己做完 5 分钟就停",
                "记录完成后身体/情绪的一点变化"
            )
        ),
        Scenario(
            id = "lonely",
            title = "我好像很孤独",
            subtitle = "想联系谁，又怕打扰",
            emoji = "🌊",
            opening = "孤独感上来的时候，会觉得自己和谁都不在同一个频道。你愿意说说，最近这种感受是从什么时候开始的吗？",
            goal = "把“没人懂我”具体化，并找到一个低门槛的连接动作。",
            steps = listOf(
                "描述孤独感最明显的时刻",
                "想想有没有一个相对安全的人",
                "设计一句很轻的开口话",
                "约定今天只发一条消息，不要求立刻回复"
            )
        ),
        Scenario(
            id = "just_listen",
            title = "只是想被听见",
            subtitle = "不用建议，只要有人在",
            emoji = "🫖",
            opening = "好，我不急着给建议。你可以慢慢说，我会认真听。",
            goal = "让用户被完整地听见，不评价、不打断。",
            steps = listOf(
                "只做反映式倾听",
                "不追问过多细节",
                "不给建议、不评判",
                "用一句简短的陪伴收住"
            )
        )
    )

    fun byId(id: String?): Scenario? = all.firstOrNull { it.id == id }
}
