package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["personId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val type: String, // "DEBTOR_HIM" (عليه) or "CREDITOR_ME" (له)
    val amount: Double,
    val currency: String = "YER", // "YER", "SAR", "USD", "GOLD_21K", "SILVER"
    val date: Long = System.currentTimeMillis(),
    val details: String = "",
    val balanceAfter: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_DEBTOR_HIM = "DEBTOR_HIM"   // عليه (مستحق لك)
        const val TYPE_CREDITOR_ME = "CREDITOR_ME" // له (التزام عليك)

        const val CURRENCY_YER = "YER"
        const val CURRENCY_SAR = "SAR"
        const val CURRENCY_USD = "USD"
        const val CURRENCY_GOLD_21K = "GOLD_21K"
        const val CURRENCY_SILVER = "SILVER"
    }
}
