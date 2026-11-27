package com.example.retailcustomermanagement

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Locale

data class Customer(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String
)

class MainActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val customers = mutableListOf<Customer>()
    private val visibleCustomers = mutableListOf<Customer>()
    private lateinit var nameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var searchInput: EditText
    private lateinit var listView: ListView
    private lateinit var statusText: TextView
    private lateinit var saveButton: Button
    private var editingId: String? = null
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        statusText.text = "Connecting to Firebase…"
        if (auth.currentUser != null) startCustomerListener()
        else auth.signInAnonymously()
            .addOnSuccessListener { startCustomerListener() }
            .addOnFailureListener { statusText.text = "Firebase sign-in failed: ${it.localizedMessage}" }
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 12)
            setBackgroundColor(0xFFF6F8FC.toInt())
        }
        val title = TextView(this).apply {
            text = "Retail Customer Management"
            textSize = 23f
            setTextColor(0xFF17365D.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        page.addView(title)
        nameInput = field("Customer name")
        phoneInput = field("Phone number", android.text.InputType.TYPE_CLASS_PHONE)
        emailInput = field("Email address", android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        addressInput = field("Address", android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        page.addView(nameInput); page.addView(phoneInput); page.addView(emailInput); page.addView(addressInput)

        saveButton = Button(this).apply { text = "Add Customer"; isAllCaps = false }
        page.addView(saveButton)
        saveButton.setOnClickListener { saveCustomer() }

        val clearButton = Button(this).apply { text = "Clear Form"; isAllCaps = false }
        page.addView(clearButton)
        clearButton.setOnClickListener { clearForm() }

        searchInput = field("Search by name, phone or email")
        page.addView(searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshList() }
            override fun afterTextChanged(s: Editable?) {}
        })

        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF526174.toInt())
            setPadding(4, 8, 4, 8)
        }
        page.addView(statusText)

        listView = ListView(this)
        page.addView(listView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        listView.setOnItemClickListener { _, _, position, _ ->
            val customer = visibleCustomers[position]
            nameInput.setText(customer.name)
            phoneInput.setText(customer.phone)
            emailInput.setText(customer.email)
            addressInput.setText(customer.address)
            editingId = customer.id
            saveButton.text = "Update Customer"
            statusText.text = "Editing ${customer.name}. Change the fields and tap Update Customer."
        }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            confirmDelete(visibleCustomers[position]); true
        }
        setContentView(page)
    }


    private fun field(
        hintText: String,
        inputType: Int = android.text.InputType.TYPE_CLASS_TEXT
    ) = EditText(this).apply {
        hint = hintText
        textSize = 15f
        this.inputType = inputType
        isSingleLine = hintText != "Address"
        setPadding(14, 8, 14, 8)
        setBackgroundColor(0xFFFFFFFF.toInt())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 4, 0, 7)
        layoutParams = params
    }


    private fun saveCustomer() {
        val name = nameInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val address = addressInput.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Customer name and phone number are required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.length < 7) {
            Toast.makeText(this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show()
            return
        }
        val data = hashMapOf(
            "name" to name,
            "phone" to phone,
            "email" to email,
            "address" to address,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        val id = editingId
        val collection = db.collection("customers")
        val document = if (id == null) collection.document() else collection.document(id)
        document.set(data)
            .addOnSuccessListener {
                Toast.makeText(this, if (id == null) "Customer added." else "Customer updated.", Toast.LENGTH_SHORT).show()
                clearForm()
            }.addOnFailureListener {
                Toast.makeText(this, "Save failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startCustomerListener() {
        listener?.remove()
        listener = db.collection("customers")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    statusText.text = "Could not load customers: ${error.localizedMessage}"
                    return@addSnapshotListener
                }
                customers.clear()
                snapshot?.documents?.forEach { doc ->
                    customers.add(
                        Customer(
                            doc.id,
                            doc.getString("name") ?: "",
                            doc.getString("phone") ?: "",
                            doc.getString("email") ?: "",
                            doc.getString("address") ?: ""
                        )
                    )
                }
                statusText.text = "Customers saved in Firebase: ${customers.size}. Tap a record to edit; long-press to delete."
                refreshList()
            }
    }

    private fun refreshList() {
        if (!::listView.isInitialized) return
        val query = if (::searchInput.isInitialized) searchInput.text.toString().trim().lowercase(Locale.getDefault()) else ""
        visibleCustomers.clear()
        visibleCustomers.addAll(customers.filter {
            it.name.lowercase(Locale.getDefault()).contains(query) ||
            it.phone.lowercase(Locale.getDefault()).contains(query) ||
            it.email.lowercase(Locale.getDefault()).contains(query)
        })
        val rows = visibleCustomers.map {
            "${it.name}\nPhone: ${it.phone}    Email: ${it.email}\nAddress: ${it.address}"
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun confirmDelete(customer: Customer) {
        AlertDialog.Builder(this)
            .setTitle("Delete customer?")
            .setMessage("Delete ${customer.name} from the customer list?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                db.collection("customers").document(customer.id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Customer deleted.", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(this, "Delete failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
            }.show()
    }

    private fun clearForm() {
        nameInput.text.clear()
        phoneInput.text.clear()
        emailInput.text.clear()
        addressInput.text.clear()
        editingId = null
        saveButton.text = "Add Customer"
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }
}
