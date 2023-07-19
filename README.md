# rememberme
### Start-App Hackathon 1st Prize:trophy:
- **Organiser**: National Program of Excellence in Software
- **Period**: 29th - 31th Oct, 2021

# Customized Welfare Policy Information Provision System

# Outline
1. An information service app that allows the socially underprivileged people to more easily access government-run welfare programmes and policies.

# Progress
1. Provide menu UI for underprivileged class.  
<img src="https://github.com/BanSangSu/rememberme/assets/76412884/e60fe329-5153-4c74-9982-2dcf8d7bc561" 
alt="menu UI" width="600" />  

3. Show policy information using RecyclerView.  
<img src="https://github.com/BanSangSu/rememberme/assets/76412884/2c88df0b-2a0d-4abe-9878-f740a901c5d5" 
alt="policy information using RecyclerView" width="600" />

5. Details can be searched by typing or using voice recognition.  
<img src="https://github.com/BanSangSu/rememberme/assets/76412884/460fca82-3e3c-4eca-93ea-6c9dafe143b3" 
alt="search" width="600"  />

7. Alert a notification push message using Reminder function.  
<img src="https://github.com/BanSangSu/rememberme/assets/76412884/4dce6dd1-37f1-4467-a36d-caa993a1ff81" 
alt="alert" width="600" />

# Revenue Model
1. Outsourcing expenses of advertisement from government.
2. Fees for government programmes and policies materials agency.

# TroubleShooting
1. Depening on permission, I could access the text with the Write and Read modes, but I couldn't access hyperlinks with the Read mode.  
  -> Only Read mode was provided.  
   &nbsp; ->> Todo) Make two layout, Read layout and Write layout.  
2. In the process of classifying categories, internal codes such as the DAO part or DTO part that deal with SQLite were read-only, so it was not possible to classify all classes in the Database in details.  
  -> We put the data into existing Database columns, classified it to a certain extent, and distributed the information using existing code that passes features through boolean values.  
   &nbsp; ->> Todo) I plan to increase productivity and scalability by building Firebase of personal DB.  
3. Even though the images were saved in the internal DB so it did not disappear when I went out and entered, when I forcibly pulled out the DB separately and built it, only the image data was not received.  
  -> I imported the DB, then took images and uploaded it.  
  &nbsp; ->> Todo) I need to build a stable DB so that I can accurately store and use various contents externally(Integrity).
4. The plan was to create a voice recognition function in a fragment and embed it as an external icon.  
  -> Fragments did not communicate data well with each other, and there were some problems with program execution. So the voice recognition function provided by the search function was used.  
   &nbsp; ->> Todo) In addition to pulling it out to an external fragment, I plan to provide a UI/UX for the visually impaired.  

# Other future
1. Receive the user's residence and region information and provide benefit information according to the location.
2. Get the user's consent to obtain user status information, and provide information accordingly.
3. Bring real-time information on government programmes through Web crawling.
4. Offer services that cut through the red tape, such as the paperwork required to receive benefits.
5. Based on the data, it would make recommendations on what materials you need and how to prepare them according to the programme.
6. Provide information services to organisations to predict and prepare for future city expenses by identifying the features of people who benefit from big data.
