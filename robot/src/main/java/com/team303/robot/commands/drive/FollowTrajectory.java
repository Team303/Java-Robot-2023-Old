package com.team303.robot.commands.drive;

//import com.team303.lib.kinematics.IKWrapper;

import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.math.trajectory.TrajectoryUtil;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.wpilibj2.command.SwerveControllerCommand;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.controller.HolonomicDriveController;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;

import java.io.FileNotFoundException;

import com.team303.robot.RobotMap.Auto;
import com.team303.robot.subsystems.SwerveSubsystem;

public class FollowTrajectory extends SwerveControllerCommand {

    public static Trajectory convert(String dir) throws FileNotFoundException {
        try {
            return TrajectoryUtil.fromPathweaverJson(Filesystem.getDeployDirectory().toPath().resolve(dir));
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileNotFoundException("Autos JSON file not found");
        }
    }

    public FollowTrajectory(String directory) throws FileNotFoundException {
        this(directory, 
            new ProfiledPIDController(0.1, 0, 0,
                new TrapezoidProfile.Constraints(
                    Auto.MAX_VELOCITY,
                    Auto.MAX_ACCELERATION
                )
            )
        );
    }

    public FollowTrajectory(String directory, ProfiledPIDController angleController) throws FileNotFoundException {
            super(
                convert(directory),
                SwerveSubsystem.getSwerve()::getPose,
                SwerveSubsystem.getSwerve().getKinematics(),
                new HolonomicDriveController(
                    new PIDController(0.1,0.0,0.0),
                    new PIDController(0.1,0.0,0.0),
                    angleController
                ),
                SwerveSubsystem.getSwerve()::drive,
                SwerveSubsystem.getSwerve()
            );
            angleController.enableContinuousInput(-Math.PI, Math.PI);
    }

}
