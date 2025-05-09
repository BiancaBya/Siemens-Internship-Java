# Siemens Java Internship – Code Refactoring Project

## Hi there 👋

This is the refactored solution for the Siemens Java Internship assignment. All required fixes, validations, tests and improvements have been applied, while maintaining the same functionality.

---

## 🛠 What’s Changed / Implemented

1. **Logical Errors Fixed**  
    - Resolved race condition where two threads could process the same item simultaneously
    - Handled responses properly
      
2. **Error Handling & Validation**  
   - Implemented custom input validation to enforce correct data formats and handle errors gracefully
     
3. **Improved Documentation**  
   - Inline comments clarifying business rules
     
4. **Comprehensive Testing**  
   - Unit tests for service & controller 
   - > 100% code coverage
     
5. **HTTP Status Codes**  
   - `201 CREATED` for resource creation  
   - `204 NO CONTENT` for successful deletions  
   - `400 BAD REQUEST` on validation errors  
   - `404 NOT FOUND` when entities are missing
     
6. **Email Validation**  
   - Custom regex validator for `email` field
     
7. **Refactored `processItemsAsync`**  
   - Uses `CompletableFuture.supplyAsync(...)` per item  
   - Aggregates with `CompletableFuture.allOf(...)`  
   - Returns `List<Item>` of all successfully processed items  


