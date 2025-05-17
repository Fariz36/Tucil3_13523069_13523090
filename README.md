# Tucil3_13523069_13523090 - Kessoku No Owari 🎸

<div align="center">
  <img src="https://github.com/user-attachments/assets/d337abaf-7a07-45d6-a4b1-2a512e11f822" alt="Ikuyoo" />
</div>

 <div align="center" id="contributor">
   <strong>
     <h3> Admin and Members </h3>
     <table align="center">
       <tr align="center">
         <td>NIM</td>
         <td>Name</td>
         <td>GitHub</td>
       </tr>
       <tr align="center">
         <td>13523069</td>
         <td>Mochammad Fariz Rifqi Rizqulloh</td>
         <td><a href="https://github.com/Fariz36">@Fariz36</a></td>
       </tr>
       <tr align="center">
         <td>13523090</td>
         <td>Nayaka Ghana Subrata</td>
         <td><a href="https://github.com/Nayekah">@Nayekah</a></td>
       </tr>
     </table>
   </strong>
 </div>

<div align="center">
  <h3>Tech Stacks and Languages</h3>

  <p>
    <img src="https://github.com/user-attachments/assets/b9b22c03-8659-4afb-9b73-863c56939830" alt="Java" width="150"/>
    <img src="https://github.com/user-attachments/assets/68aea290-2c23-423a-ad41-3a3eac88e71c" alt="Gradle" width="100"/>
  </p>
</div>

 <p align="center">
    <br />
    <a href="https://youtu.be/7FDRQifEMUQ?si=gKheP3GnBORXsDY4">Kessoku!</a>
    ·
    <a href="https://github.com/Fariz36/Tucil3_13523069_13523090/tree/main/doc/.pdf">Project Report (Bahasa Indonesia)</a>
</p>

## Intermezzo

<div style="text-align: justify">

The Kessoku Band: Hitori "Bocchi" Gotoh, Ryo Yamada, Nijika Ijichi, and Kita Ikuyo—are driving to a big gig at Starry in Shimo-Kitazawa. Their van, carrying their instruments and Bocchi's nerves, gets stuck in a traffic jam on a MxN grid of streets. Cars and trucks block the way, some moving side-to-side, others up-and-down. The band's van (the "red car") needs to slide horizontally to reach the exit on the right side, with as few moves as possible, to make it to the show on time.

Bocchi panics but suggests, "C-can we solve this like a puzzle?" Nijika nods, "Let's find the fastest way out!" The band uses three pathfinding tricks, each matching their vibe:

1. **A* Search**: Nijika's smart planning takes charge. She picks moves that balance how many steps they've taken and how close they are to the exit, like hitting the perfect drum rhythm. A* Search is efficient, guiding the van smoothly to Starry.

2. **Uniform Cost Search (UCS)**: Ryo, always chill, says, "Just take the cheapest path, step by step." UCS checks every move to find the one with the lowest total cost, like Ryo carefully budgeting her snacks. It's steady but takes its time.

3. **Greedy Best-First Search**: Kita's enthusiasm shines. "Go straight for the exit, full speed!" she cheers. This method picks moves that seem closest to the goal, like Kita's bold guitar strums, but it might miss the best path by rushing.

The band works together, sliding cars and trucks out of the way. Nijika's A* keeps them on track, Ryo's UCS ensures no wasted moves, and Kita's Greedy Search adds speed. Bocchi, less shaky now, helps spot key moves. Finally, the van zooms through the exit, and they arrive at Starry just in time.

</div>

<p align="center">
  <img src="https://github.com/user-attachments/assets/c267a42b-74f6-4d1f-8d42-fde28c104a29" alt="Kessoku Band solving the traffic puzzle" width="600">
</p>

## Installation & Setup
 
### Requirements
- Git
- Java 11

### Dependencies
- Gradle 8.7

<br/>

### Installing Dependencies

<a id="dependencies"></a>
> [!IMPORTANT]  
> If you're using linux (mainly Ubuntu or Debian distro), make sure that java 11 is installed, or you can do:
   ```
   sudo apt update
   sudo apt install openjdk-11-jdk
   sudo apt install gradle
```
> For backend, just install golang: https://go.dev/dl/
> For production, please refer to "How to Run" section

---
 ## How to Run
 ### Command Line Interface (Development)
 1. Open a terminal
 2. Clone the repository (if not already cloned)
       ```bash
    git clone https://github.com/Fariz36/Tucil3_13523069_13523090.git
    
 3. go to Tucil3_13523069_13523090 directory:
       ```bash
    cd Tucil3_13523069_13523090
    
 4. Install the [dependencies](#dependencies) first
 5. Do: 
    ```bash
    # Windows
    .\kessoku.bat cli

    # Linux
    ./kessoku cli

 ### Graphical User Interface
 1. Open a terminal
 2. Clone the repository (if not already cloned)
       ```bash
    git clone https://github.com/Fariz36/Tucil3_13523069_13523090.git
    
 3. go to Tucil3_13523069_13523090 directory:
       ```bash
    cd Tucil3_13523069_13523090
    
 4. Install the [dependencies](#dependencies) first
 5. Do: 
    ```bash
    # Windows
    .\kessoku.bat gui

    # Linux
    ./kessoku gui

> [!Note]
> Make sure that all of the dependencies are already installed

---
 ## Build and Clean
 1. Open a terminal
 2. Clone the repository (if not already cloned)
       ```bash
    git clone https://github.com/Fariz36/Tucil3_13523069_13523090.git
    
 3. go to Tucil3_13523069_13523090 directory:
       ```bash
    cd Tucil3_13523069_13523090
 5. Install the [dependencies](#dependencies) first (prerequisite for "build" command)

 7. Do: 
    ```bash
    Cleaning exxecutable in bin directory
    # Windows
    .\kessoku.bat clean

    # Linux
    ./kessoku clean


    Building executable to bin directory
    # Windows
    .\kessoku.bat build

    # Linux
    ./kessoku build

 <br/>
 <br/>
 <br/>
 <br/>
 
 <div align="center">
 Strategi Algoritma • © 2025 • Kessoku No Owari
 </div>
