# Point Cluster and Collision Simulation Repository

Welcome to the Point Cluster and Collision Simulation repository. This simulation system visualizes points that are grouped into clusters. The points undergo various operations, such as collision detection, movement within a space, and rotation around their center of mass. The simulation takes advantage of multi-threading to optimize performance.

## Table of Contents

- [Configuration](#configuration)
    - [Drawing Settings](#drawing-settings)
    - [Point Settings](#point-settings)
    - [Cluster Settings](#cluster-settings)
    - [Thread Settings](#thread-settings)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)

## Configuration

The behavior of the simulation can be fine-tuned using the provided `.properties` file. Below is a breakdown of the available configuration options:

### Drawing Settings
- `draw.compression.quality`: Sets the drawing compression quality. Default value: `0.96`.
- `draw.scale.factor`: Specifies the magnification factor for each image; a downscaling follows this process. Default value: `2.0`.
- `draw.scale.point`: Determines the scaling factor for the points. The base is the radius from the next section, and this option enhances the visibility of points during the display process. Default value: `4.0`.

### Point Settings
- `points.number`: Defines the number of points to be generated and processed. Default value: `40000`.
- `points.radius`: Sets the radius of the points based on which collisions are detected. Default value: `2.0`.

### Cluster Settings
- `cluster.lifetime`: Represents the lifetime of clusters. It defines the number of frames a point will surely belong to a cluster, after which it might separate from it. Default value: `3`.

### Thread Settings
- `threads.parts.number`: Indicates the number of tasks for workload distribution among various threads. Default value: `128`.
- `threads.number`: Specifies the number of threads used for parallel processing. Default value: `8`.
- `threads.drawCount`: Dictates the maximum number of threads that can be engaged in drawing images simultaneously. Default value: `6`.

## Installation


1. Clone the repository: 
```
git clone [URL to the repository]
```

2. Navigate to the cloned directory:
```
cd Point-Cluster-and-Collision-Simulation
```

## Usage

[Provide a brief overview of how to run the simulation or use the application.]

## License

This project is licensed under the [MIT License](LICENSE). Check the LICENSE file for more details.
