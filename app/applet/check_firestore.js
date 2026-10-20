const fs = require('fs');

async function checkFirestore() {
  const apiKey = "AIzaSyApWemMXKABShdzN68dx5n3hdMpJS4-8yA";
  const baseUrl = "https://firestore.googleapis.com/v1/projects/oikonomos-af58b/databases/(default)/documents";

  async function get(path) {
    const res = await fetch(`${baseUrl}${path}?key=${apiKey}`);
    if (!res.ok) return { status: res.status, error: await res.text() };
    return await res.json();
  }

  console.log("=== CHECKING USERS ===");
  const users = await get("/users");
  if (users.documents) {
    for (const u of users.documents) {
      console.log("User:", u.name.split("/").pop());
      console.log("  email:", u.fields?.email?.stringValue);
      console.log("  displayName:", u.fields?.displayName?.stringValue);
      console.log("  householdId:", u.fields?.householdId?.stringValue);
      console.log("  householdIds:", JSON.stringify(u.fields?.householdIds));

      const uPath = u.name.split("/documents")[1];
      const uExpenses = await get(`${uPath}/expenses`);
      console.log(`  User direct expenses for ${uPath}:`, (uExpenses.documents || []).length);
    }
  }

  console.log("=== CHECKING HOUSEHOLDS ===");
  const households = await get("/households");
  if (households.documents) {
    for (const h of households.documents) {
      const hId = h.name.split("/").pop();
      console.log("Household:", hId, "name:", h.fields?.name?.stringValue, "owner:", h.fields?.ownerId?.stringValue, "members:", JSON.stringify(h.fields?.memberUids));
      
      const hExpenses = await get(`/households/${hId}/expenses`);
      console.log(`  Expenses for ${hId}:`, (hExpenses.documents || []).length);
      if (hExpenses.documents && hExpenses.documents.length > 0) {
        console.log(`    First expense title:`, hExpenses.documents[0].fields?.title?.stringValue, "amount:", JSON.stringify(hExpenses.documents[0].fields?.amount));
      }

      const hMembers = await get(`/households/${hId}/members`);
      console.log(`  Members for ${hId}:`, (hMembers.documents || []).length);

      const hCats = await get(`/households/${hId}/categories`);
      console.log(`  Categories for ${hId}:`, (hCats.documents || []).length);
    }
  }
}

checkFirestore().catch(console.error);
