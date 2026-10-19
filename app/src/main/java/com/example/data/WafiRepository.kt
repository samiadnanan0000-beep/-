package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.abs

data class PersonWithBalance(
    val person: PersonEntity,
    val balancesByCurrency: Map<String, Double>, // e.g. "YER" -> 150000.0, "GOLD_21K" -> 25.5
    val transactionCount: Int,
    val lastTransactionDate: Long?
) {
    fun getPrimaryBalance(preferredCurrency: String): Double {
        return balancesByCurrency[preferredCurrency] ?: balancesByCurrency.values.firstOrNull() ?: 0.0
    }

    fun isCleared(): Boolean {
        return balancesByCurrency.values.all { abs(it) < 0.0001 }
    }
}

data class GlobalFinancialSummary(
    val totalReceivable: Double, // لك (عليه)
    val totalPayable: Double,    // عليك (له)
    val netBalance: Double,
    val currency: String
)

class WafiRepository(private val dao: WafiDao) {

    val allPersonsFlow: Flow<List<PersonEntity>> = dao.getAllPersonsFlow()
    val allTransactionsFlow: Flow<List<TransactionEntity>> = dao.getAllTransactionsFlow()

    fun getPersonsWithBalancesFlow(preferredCurrency: String = "YER"): Flow<List<PersonWithBalance>> {
        return combine(dao.getAllPersonsFlow(), dao.getAllTransactionsFlow()) { persons, transactions ->
            val txByPerson = transactions.groupBy { it.personId }
            persons.map { person ->
                val pTx = txByPerson[person.id] ?: emptyList()
                val balances = mutableMapOf<String, Double>()
                for (tx in pTx) {
                    val current = balances.getOrDefault(tx.currency, 0.0)
                    val delta = if (tx.type == TransactionEntity.TYPE_DEBTOR_HIM) tx.amount else -tx.amount
                    balances[tx.currency] = current + delta
                }
                PersonWithBalance(
                    person = person,
                    balancesByCurrency = balances,
                    transactionCount = pTx.size,
                    lastTransactionDate = pTx.maxByOrNull { it.date }?.date
                )
            }
        }
    }

    fun getTransactionsForPerson(personId: Long): Flow<List<TransactionEntity>> {
        return dao.getTransactionsForPersonFlow(personId)
    }

    suspend fun getPersonById(id: Long): PersonEntity? = dao.getPersonById(id)

    suspend fun addPerson(name: String, phone: String = "", creditLimit: Double = 0.0, notes: String = ""): Long {
        val person = PersonEntity(
            name = name.trim(),
            phone = phone.trim(),
            creditLimit = creditLimit,
            notes = notes.trim()
        )
        return dao.insertPerson(person)
    }

    suspend fun updatePerson(person: PersonEntity) {
        dao.updatePerson(person)
    }

    suspend fun deletePerson(person: PersonEntity) {
        dao.deletePerson(person)
    }

    suspend fun addTransaction(
        personId: Long,
        type: String,
        amount: Double,
        currency: String,
        details: String = "",
        date: Long = System.currentTimeMillis()
    ): Long {
        // Calculate cumulative balance after this transaction
        val existing = dao.getTransactionsForPersonList(personId)
            .filter { it.currency == currency }
        val currentBalance = existing.sumOf {
            if (it.type == TransactionEntity.TYPE_DEBTOR_HIM) it.amount else -it.amount
        }
        val delta = if (type == TransactionEntity.TYPE_DEBTOR_HIM) amount else -amount
        val balanceAfter = currentBalance + delta

        val tx = TransactionEntity(
            personId = personId,
            type = type,
            amount = amount,
            currency = currency,
            details = details,
            date = date,
            balanceAfter = balanceAfter
        )
        val id = dao.insertTransaction(tx)
        recalculatePersonBalances(personId, currency)
        return id
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        dao.deleteTransaction(tx)
        recalculatePersonBalances(tx.personId, tx.currency)
    }

    suspend fun recalculatePersonBalances(personId: Long, currency: String) {
        val transactions = dao.getTransactionsForPersonList(personId)
            .filter { it.currency == currency }
            .sortedWith(compareBy({ it.date }, { it.id }))

        var runningBalance = 0.0
        for (item in transactions) {
            val delta = if (item.type == TransactionEntity.TYPE_DEBTOR_HIM) item.amount else -item.amount
            runningBalance += delta
            if (item.balanceAfter != runningBalance) {
                dao.updateTransaction(item.copy(balanceAfter = runningBalance))
            }
        }
    }

    suspend fun exportDataToJson(): String {
        val persons = dao.getAllPersonsList()
        val transactions = dao.getAllTransactionsList()
        val sb = StringBuilder()
        sb.append("{\n  \"version\": 1,\n  \"exportedAt\": ${System.currentTimeMillis()},\n")
        sb.append("  \"persons\": [\n")
        persons.forEachIndexed { i, p ->
            sb.append("    {\"id\": ${p.id}, \"name\": \"${escapeJson(p.name)}\", \"phone\": \"${escapeJson(p.phone)}\", \"creditLimit\": ${p.creditLimit}, \"notes\": \"${escapeJson(p.notes)}\", \"createdAt\": ${p.createdAt}}")
            if (i < persons.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n  \"transactions\": [\n")
        transactions.forEachIndexed { i, t ->
            sb.append("    {\"id\": ${t.id}, \"personId\": ${t.personId}, \"type\": \"${t.type}\", \"amount\": ${t.amount}, \"currency\": \"${t.currency}\", \"date\": ${t.date}, \"details\": \"${escapeJson(t.details)}\", \"balanceAfter\": ${t.balanceAfter}}")
            if (i < transactions.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n}")
        return sb.toString()
    }

    suspend fun restoreDataFromJson(jsonString: String): Boolean {
        return try {
            val orgJson = org.json.JSONObject(jsonString)
            val personsArray = orgJson.getJSONArray("persons")
            val transactionsArray = orgJson.getJSONArray("transactions")

            val newPersons = mutableListOf<PersonEntity>()
            for (i in 0 until personsArray.length()) {
                val obj = personsArray.getJSONObject(i)
                newPersons.add(
                    PersonEntity(
                        id = obj.optLong("id", 0),
                        name = obj.getString("name"),
                        phone = obj.optString("phone", ""),
                        creditLimit = obj.optDouble("creditLimit", 0.0),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val newTransactions = mutableListOf<TransactionEntity>()
            for (i in 0 until transactionsArray.length()) {
                val obj = transactionsArray.getJSONObject(i)
                newTransactions.add(
                    TransactionEntity(
                        id = obj.optLong("id", 0),
                        personId = obj.getLong("personId"),
                        type = obj.getString("type"),
                        amount = obj.getDouble("amount"),
                        currency = obj.optString("currency", "YER"),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        details = obj.optString("details", ""),
                        balanceAfter = obj.optDouble("balanceAfter", 0.0)
                    )
                )
            }

            dao.clearAllTransactions()
            dao.clearAllPersons()
            dao.insertPersonsList(newPersons)
            dao.insertTransactionsList(newTransactions)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
