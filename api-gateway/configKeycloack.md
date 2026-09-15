# Handleiding Keycloak Configuratie
## FIOD Certiflow Project

### Keycloak opstarten
1. `docker compose up`
2. Ga naar: http://localhost:8081.
   Log in met: Username = `admin`, Password `admin`
![1.png](../../1.png)
--> **Klik op Next**


### Een client aanmaken
1. Klik op **Create Client**.
   Vul de clientgegevens in zoals weergegeven in de screenshots
4. ![2.png](../../2.png)
--> **Klik op Next**


5. Configureer de client capabilities volgens de screenshots: 
![3.png](../../3.png)
--> **Klik op Next**

6. Configureer de client capabilities volgens de screenshots.
![4.png](../../4.png)
   --> **Klik op Next**

7. Configureer de login-instellingen.
![5.png](../../5.png)
   --> **Klik op Next**

8. Controleer de instellingen en sla deze opnieuw op indien nodig.
![6.png](../../6.png)
  --> Klik op *Save*.

## Rollen aanmaken
9. Ga naar Roles. 
![7.png](../../7.png)
   --> **Klik op Next**

10. ![8.png](../../8.png)
    --> Klik op *Save**  
    
_ROLES_
- BEHEERDER
- TOEZICHTHOUDER
- LEIDINGGEVENDE
- MEDEWERKER
- ALLEEN_LEZEN
- FUNCTIONEEL_BEHEERDER
- SYSTEEMBEHEERDER

    Klik na het invoeren van iedere rol op Save.
    Herhaal dit proces totdat alle rollen zijn aangemaakt.

![9.png](../../9.png)

--> Klik op *Save**

### Gebruikers aanmaken
12. Ga naar Users
13. Klik op *Create User*.
14. Vul alle verplichte velden in. 
![10.png](../../10.png)
--> *Create user*


13. Invullen van user data (moet alle velden ingevuld zijn, ander werkt niet sopepel)
![11.png](../../11.png)
--> *Create* daarna als je wil --> *Save*
    Herhaal dit proces voor alle gebruikers die je wilt aanmaken.
![12.png](../../12.png)

### Rollen toewijzen aan gebruikers
14. Open de gebruiker waarvoor je rollen wilt configureren.
15. Ga naar het tabblad Role Mapping.
![13.png](../../13.png)

![14.png](../../14.png)

![15.png](../../15.png)
Selecteer de gewenste rol(len).
--> Klik op *Assign Role*.
Herhaal deze stappen voor iedere gebruiker.

#### Optioneel
Indien gewenst kunnen de standaardrollen van Keycloak, zoals:
`default-roles-certiflow`
worden verwijderd.

![16.png](../../16.png)
17. Herhaald deze proces voor voor je gebruikers als je wil role assign. 

### Wachtwoord instellen
18. Open de gewenste gebruiker.
19. Ga naar het tabblad Credentials.
![17.png](../../17.png)

Vul het gewenste wachtwoord in.
Bevestig het wachtwoord.
![18.png](../../18.png)
--> klip op *Save*

Configuratie voltooid

Gefeliciteerd! De basisconfiguratie van Keycloak is nu gereed.



### Authenticatie testen
#### OpenID Connect-configuratie controleren
Open de OpenID Endpoint Configuration in Keycloak.
Controleer of de waarden overeenkomen met de configuratie in jouw application.yaml.
![20.png](../../20.png)


De volgende instellingen moeten overeenkomen:

issuer-uri
authorization-uri

![21.png](../../21.png)


### Een token ophalen
Kopieer de Token Endpoint URL uit de OpenID Configuration.
Open Postman (of een vergelijkbare tool).
Vraag een access token op via het token endpoint.
![22.png](../../22.png)

Kopieer het ontvangen JWT-token.
![23.png](../../23.png)

### JWT-token controleren
ga naar https://www.jwt.io/
Controleer of de claims correct zijn.
Controleer onder andere:

sub
preferred_username
realm_access
roles

![24.png](../../24.png)

De toegewezen rollen moeten zichtbaar zijn in het token.

### Backend testen
- Start de Spring Boot applicatie:'mvn spring-boot:run' 
- Controleer dat de Keycloak-configuratie correct is ingesteld.
  Open Postman.
  Voeg het token toe als Bearer Token.
  Verstuur een request naar een beveiligde API.

![25.png](../../25.png)

Succes, 200 OK

Resultaat

Wanneer je succesvol een JWT-token kunt verkrijgen en beveiligde endpoints kunt aanroepen, is de integratie tussen Keycloak en Certiflow correct geconfigureerd.

Je applicatie werkt nu met Keycloak. Gefeliciteerd! 🎉
