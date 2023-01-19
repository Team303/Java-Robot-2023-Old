package com.team303.robot;

import com.kauailabs.navx.frc.AHRS;
import com.team303.robot.RobotMap.IOConstants;
import com.team303.robot.RobotMap.LED;
import com.team303.robot.autonomous.Autonomous;
import com.team303.robot.autonomous.AutonomousProgram;
import com.team303.robot.commands.drive.DefaultDrive;
import com.team303.robot.commands.drive.DriveWait;
import com.team303.robot.commands.drive.FollowTrajectory;
import com.team303.robot.commands.led.LEDSolidColor;
//import com.team303.robot.subsystems.ArmSubsystem;
import com.team303.robot.subsystems.SwerveSubsystem;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.math.geometry.Rotation2d;
import java.util.List;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.math.trajectory.Trajectory;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.networktables.NT4Publisher;


public class Robot extends LoggedRobot {

	/* RoboRio Sensors */
	private static final AHRS navX = new AHRS();

	/* Robot IO Controls */
	private static final Joystick leftJoystick = new Joystick(IOConstants.LEFT_JOYSTICK_ID);
	private static final Joystick rightJoystick = new Joystick(IOConstants.RIGHT_JOYSTICK_ID);

	/* Shufflebaord Tabs */
	public static final ShuffleboardTab AUTO_TAB = Shuffleboard.getTab("Autonomous");

	/* Shuffleboard Choosers */
	public static SendableChooser<Double> autoDelayChooser = new SendableChooser<>();

	/* Robot alliance color */
	public static Color allianceColor = DriverStation.getAlliance() == Alliance.Blue ? LED.RED : LED.BLUE;

	private static final NetworkTableInstance inst = NetworkTableInstance.getDefault();;

	/*Getter Methods*/

	public static AHRS getNavX() {
		return navX;
	}

	public static Joystick getRightJoyStick() {
		return rightJoystick;
	}

	public static Joystick getLeftJoyStick() {
		return leftJoystick;
	}

	public static NetworkTableInstance getNetworkTableInstance() {
		return inst;
	}

	/**
	 * Defines all the options for the autonomous delay
	 */
	static {
		for (double i = 0; i < 15; i += 0.25)
			autoDelayChooser.addOption(String.format("%.2f", i), i);

		autoDelayChooser.setDefaultOption("0.0", 0.0D);

		AUTO_TAB.add("Auto Start Delay", autoDelayChooser);
	}

	// The command configured to run during auto
	private Command autonomousCommand;
;

	@Override
	public void robotInit() {
		Logger logger = Logger.getInstance();

		// Record metadata
		logger.recordMetadata("ProjectName", BuildConstants.MAVEN_NAME);
		logger.recordMetadata("BuildDate", BuildConstants.BUILD_DATE);
		logger.recordMetadata("GitSHA", BuildConstants.GIT_SHA);
		logger.recordMetadata("GitDate", BuildConstants.GIT_DATE);
		logger.recordMetadata("GitBranch", BuildConstants.GIT_BRANCH);
		switch (BuildConstants.DIRTY) {
			case 0:
			logger.recordMetadata("GitDirty", "All changes committed");
			break;
			case 1:
			logger.recordMetadata("GitDirty", "Uncomitted changes");
			break;
			default:
			logger.recordMetadata("GitDirty", "Unknown");
			break;
		}

		    // Set up data receivers & replay source
		if (Robot.isReal()) {
			// Running on a real robot, log to a USB stick
			logger.addDataReceiver(new WPILOGWriter("/media/sda1/"));
			logger.addDataReceiver(new NT4Publisher());
		}
		else {
			logger.addDataReceiver(new WPILOGWriter(""));
			logger.addDataReceiver(new NT4Publisher());
		}

		// Configure the joystick and controller bindings
		configureButtonBindings();

		//set default commands
		SwerveSubsystem.getSwerve().setDefaultCommand(new DefaultDrive(true));

		//add Autos to Shuffleboard
		Autonomous.init();
		AutonomousProgram.addAutosToShuffleboard();

		// Start Camera
		if (Robot.isReal())
			CameraServer.startAutomaticCapture();

		logger.start();
	}

	@Override
	public void autonomousInit() {
		// Chooses which auto we do from SmartDashboard
		autonomousCommand = AutonomousProgram.autoChooser.getSelected().construct();

		// Schedule the selected autonomous command group
		if (autonomousCommand != null) {
			CommandScheduler.getInstance().schedule(
					// To achieve the configured delay, use a sequential group that contains a wait
					// command
					new SequentialCommandGroup(
							new DriveWait(autoDelayChooser.getSelected()),
							autonomousCommand));
		}

		// Match LEDs color to team
		CommandScheduler.getInstance().schedule(new LEDSolidColor(allianceColor));
	}

	@Override
	public void teleopInit() {
		// This makes sure that the autonomous stops running when teleop starts running.
		if (autonomousCommand != null)
			autonomousCommand.cancel();

		// Match LEDs color to team
		CommandScheduler.getInstance().schedule(new LEDSolidColor(allianceColor));
		
	}

	@Override
	public void disabledInit() {
		// Change LED color to signify disabled state
		CommandScheduler.getInstance().schedule(new LEDSolidColor(LED.DISABLED_COLOR));
	}

	@Override
	public void disabledPeriodic() {
	}

	private void configureButtonBindings() {

	}

	
	@Override
	public void simulationInit() {

		//set default commands
		SwerveSubsystem.getSwerve().setDefaultCommand(new DefaultDrive(true));

		//Path Weaver Trajectory
		try {
			Trajectory trajectory = FollowTrajectory.convert("output/Test.wpilib.json");
			// Push the trajectory to Field2d.
			SwerveSubsystem.field.getObject("traj").setTrajectory(trajectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override 
	public void simulationPeriodic() {

	}

	/*
	 * This Robot is configured to run with the WPILib CommandScheduler.
	 * ⛔ Nothing should be handled in the below methods ⛔
	 */

	@Override
	public void robotPeriodic() {
		/*
		 * Runs the Scheduler. This is responsible for polling buttons, adding
		 * newly-scheduled
		 * commands, running already-scheduled commands, removing finished or
		 * interrupted commands,
		 * and running subsystem periodic() methods. This must be called from the
		 * robot's periodic
		 * block in order for anything in the Command-based framework to work.
		 */

		try {
			CommandScheduler.getInstance().run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}