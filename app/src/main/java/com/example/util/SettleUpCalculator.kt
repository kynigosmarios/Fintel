package com.example.util

import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.data.entity.getPayerShares
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

data class MemberBalanceSummary(
    val member: FamilyMember,
    val totalPaid: Double,
    val totalShare: Double,
    val netBalance: Double // > 0 means creditor (owed money), < 0 means debtor (owes money)
)

data class SettlementTransfer(
    val fromMember: FamilyMember,
    val toMember: FamilyMember,
    val amount: Double
)

data class SettleUpResult(
    val memberSummaries: List<MemberBalanceSummary>,
    val transfers: List<SettlementTransfer>,
    val totalHouseholdExpenses: Double,
    val isBalanced: Boolean
)

object SettleUpCalculator {

    fun calculate(
        expenses: List<Expense>,
        members: List<FamilyMember>
    ): SettleUpResult {
        if (members.isEmpty() || expenses.isEmpty()) {
            return SettleUpResult(
                memberSummaries = members.map {
                    MemberBalanceSummary(
                        member = it,
                        totalPaid = 0.0,
                        totalShare = 0.0,
                        netBalance = 0.0
                    )
                },
                transfers = emptyList(),
                totalHouseholdExpenses = 0.0,
                isBalanced = true
            )
        }

        val activeMembers = members
        val memberMap = activeMembers.associateBy { it.id }

        // 1. Tally payments and fair shares
        val paidMap = mutableMapOf<Long, Double>()
        val shareMap = mutableMapOf<Long, Double>()
        activeMembers.forEach { m ->
            paidMap[m.id] = 0.0
            shareMap[m.id] = 0.0
        }

        var totalExpenses = 0.0

        for (expense in expenses) {
            val amount = expense.amount
            if (amount <= 0.0) continue
            totalExpenses += amount

            // A. Credit payer(s)
            val payerShares = expense.getPayerShares(activeMembers)
            if (payerShares.isNotEmpty()) {
                for (share in payerShares) {
                    val payerId = share.memberId
                    if (paidMap.containsKey(payerId)) {
                        paidMap[payerId] = (paidMap[payerId] ?: 0.0) + share.amount
                    } else {
                        // Fallback: match by name
                        val found = activeMembers.find { it.name.equals(share.memberName, ignoreCase = true) }
                        if (found != null) {
                            paidMap[found.id] = (paidMap[found.id] ?: 0.0) + share.amount
                        } else if (activeMembers.isNotEmpty()) {
                            // Default to first member
                            val fallbackId = activeMembers.first().id
                            paidMap[fallbackId] = (paidMap[fallbackId] ?: 0.0) + share.amount
                        }
                    }
                }
            } else {
                // Single payer fallback
                val fallbackId = if (expense.payerMemberId != 0L && paidMap.containsKey(expense.payerMemberId)) {
                    expense.payerMemberId
                } else {
                    activeMembers.first().id
                }
                paidMap[fallbackId] = (paidMap[fallbackId] ?: 0.0) + amount
            }

            // B. Debit beneficiary shares
            val beneficiaries = resolveBeneficiaries(expense, activeMembers)
            if (beneficiaries.isNotEmpty()) {
                val perPerson = amount / beneficiaries.size
                for (beneficiary in beneficiaries) {
                    shareMap[beneficiary.id] = (shareMap[beneficiary.id] ?: 0.0) + perPerson
                }
            } else {
                // If no specific beneficiary, split equally among all members
                val perPerson = amount / activeMembers.size
                for (m in activeMembers) {
                    shareMap[m.id] = (shareMap[m.id] ?: 0.0) + perPerson
                }
            }
        }

        // 2. Compute net balances
        val summaries = activeMembers.map { member ->
            val paid = round2(paidMap[member.id] ?: 0.0)
            val share = round2(shareMap[member.id] ?: 0.0)
            val net = round2(paid - share)
            MemberBalanceSummary(
                member = member,
                totalPaid = paid,
                totalShare = share,
                netBalance = net
            )
        }

        // 3. Greedy Debt Simplification
        val debtors = mutableListOf<Pair<FamilyMember, Double>>()
        val creditors = mutableListOf<Pair<FamilyMember, Double>>()

        for (summary in summaries) {
            if (summary.netBalance < -0.01) {
                debtors.add(Pair(summary.member, abs(summary.netBalance)))
            } else if (summary.netBalance > 0.01) {
                creditors.add(Pair(summary.member, summary.netBalance))
            }
        }

        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val transfers = mutableListOf<SettlementTransfer>()
        var dIndex = 0
        var cIndex = 0

        while (dIndex < debtors.size && cIndex < creditors.size) {
            val (debtor, debtAmount) = debtors[dIndex]
            val (creditor, creditAmount) = creditors[cIndex]

            val settleAmount = round2(min(debtAmount, creditAmount))
            if (settleAmount > 0.01) {
                transfers.add(
                    SettlementTransfer(
                        fromMember = debtor,
                        toMember = creditor,
                        amount = settleAmount
                    )
                )
            }

            val remainingDebt = round2(debtAmount - settleAmount)
            val remainingCredit = round2(creditAmount - settleAmount)

            if (remainingDebt <= 0.01) {
                dIndex++
            } else {
                debtors[dIndex] = Pair(debtor, remainingDebt)
            }

            if (remainingCredit <= 0.01) {
                cIndex++
            } else {
                creditors[cIndex] = Pair(creditor, remainingCredit)
            }
        }

        return SettleUpResult(
            memberSummaries = summaries,
            transfers = transfers,
            totalHouseholdExpenses = round2(totalExpenses),
            isBalanced = transfers.isEmpty()
        )
    }

    private fun resolveBeneficiaries(
        expense: Expense,
        activeMembers: List<FamilyMember>
    ): List<FamilyMember> {
        val splitType = expense.splitType
        if (splitType == "ALL_FAMILY" || splitType == "ALL") {
            return activeMembers
        }

        // Check memberIdsCsv
        if (expense.memberIdsCsv.isNotBlank()) {
            val ids = expense.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
            val matched = activeMembers.filter { ids.contains(it.id) }
            if (matched.isNotEmpty()) return matched
        }

        // Check memberNamesDisplay
        val raw = expense.memberNamesDisplay.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
        if (raw.isNotBlank() && !raw.equals("Κοινό Οικογένειας", ignoreCase = true) && !raw.equals("Family", ignoreCase = true)) {
            val parts = raw.split(Regex("[,&+]+|\\s+και\\s+|\\s+and\\s+")).map { it.trim() }.filter { it.isNotBlank() }
            val matched = activeMembers.filter { m ->
                parts.any { p ->
                    m.name.equals(p, ignoreCase = true) || m.name.contains(p, ignoreCase = true) || p.contains(m.name, ignoreCase = true)
                }
            }
            if (matched.isNotEmpty()) return matched
        }

        return activeMembers
    }

    fun formatSummaryText(
        result: SettleUpResult,
        householdTitle: String,
        isGreek: Boolean = true
    ): String {
        val sb = StringBuilder()
        if (isGreek) {
            sb.appendLine("📊 ΕΚΚΑΘΑΡΙΣΗ ΕΞΟΔΩΝ - $householdTitle")
            sb.appendLine("────────────────────────────")
            sb.appendLine("Σύνολο Εξόδων: ${String.format(Locale.GERMANY, "%.2f €", result.totalHouseholdExpenses)}")
            sb.appendLine()
            sb.appendLine("👤 Συνοπτική Εικόνα:")
            for (s in result.memberSummaries) {
                val status = when {
                    s.netBalance > 0.01 -> "(+${String.format(Locale.GERMANY, "%.2f €", s.netBalance)} επιστροφή)"
                    s.netBalance < -0.01 -> "(-${String.format(Locale.GERMANY, "%.2f €", abs(s.netBalance))} οφειλή)"
                    else -> "(τακτοποιημένο)"
                }
                sb.appendLine("• ${s.member.name}: Πλήρωσε ${String.format(Locale.GERMANY, "%.2f €", s.totalPaid)} | Μερίδιο ${String.format(Locale.GERMANY, "%.2f €", s.totalShare)} $status")
            }
            sb.appendLine()
            if (result.transfers.isEmpty()) {
                sb.appendLine("✅ Όλοι οι λογαριασμοί είναι απόλυτα ισοσκελισμένοι!")
            } else {
                sb.appendLine("🤝 Προτεινόμενες Μεταφορές:")
                for (t in result.transfers) {
                    sb.appendLine("👉 ${t.fromMember.name} ➜ ${t.toMember.name}: ${String.format(Locale.GERMANY, "%.2f €", t.amount)}")
                }
            }
            sb.appendLine("────────────────────────────")
            sb.appendLine("Υπολογίστηκε αυτόματα από το Fintel")
        } else {
            sb.appendLine("📊 EXPENSE SETTLEMENT - $householdTitle")
            sb.appendLine("────────────────────────────")
            sb.appendLine("Total Expenses: ${String.format(Locale.US, "%.2f €", result.totalHouseholdExpenses)}")
            sb.appendLine()
            sb.appendLine("👤 Member Breakdown:")
            for (s in result.memberSummaries) {
                val status = when {
                    s.netBalance > 0.01 -> "(+${String.format(Locale.US, "%.2f €", s.netBalance)} get back)"
                    s.netBalance < -0.01 -> "(-${String.format(Locale.US, "%.2f €", abs(s.netBalance))} owes)"
                    else -> "(settled)"
                }
                sb.appendLine("• ${s.member.name}: Paid ${String.format(Locale.US, "%.2f €", s.totalPaid)} | Share ${String.format(Locale.US, "%.2f €", s.totalShare)} $status")
            }
            sb.appendLine()
            if (result.transfers.isEmpty()) {
                sb.appendLine("✅ All balances are settled up!")
            } else {
                sb.appendLine("🤝 Recommended Transfers:")
                for (t in result.transfers) {
                    sb.appendLine("👉 ${t.fromMember.name} ➜ ${t.toMember.name}: ${String.format(Locale.US, "%.2f €", t.amount)}")
                }
            }
            sb.appendLine("────────────────────────────")
            sb.appendLine("Calculated automatically via Fintel")
        }
        return sb.toString()
    }

    private fun round2(v: Double): Double {
        return (v * 100.0).roundToInt() / 100.0
    }
}
