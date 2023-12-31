package frc.robot;

import frc.robot.Constants.DriverControlConsts;
import frc.robot.Constants.SwerveConsts;
import frc.robot.commands.AutonomousCommands.*;
import frc.robot.commands.ClawCommands.*;
import frc.robot.commands.CommandGroups.*;
import frc.robot.commands.DriveCommands.*;
import frc.robot.commands.ElevatorCommands.*;
import frc.robot.commands.PivotCommands.*;
import frc.robot.commands.LED_Commands.*;
import frc.robot.subsystems.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import com.pathplanner.lib.PathPlannerTrajectory;
import com.pathplanner.lib.PathConstraints;
import com.pathplanner.lib.PathPlanner;
import com.pathplanner.lib.auto.PIDConstants;
import com.pathplanner.lib.auto.SwerveAutoBuilder;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.PrintCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.POVButton;

public class RobotContainer {
  public static SwerveSubsystem swerveSubsystem = new SwerveSubsystem();
  public static ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();
  public static PivotSubsystem pivotSubsystem = new PivotSubsystem();
  public static ClawSubsystem clawSubsystem = new ClawSubsystem();
  public static Lights lights = new Lights();

  private XboxController xbox = new XboxController(DriverControlConsts.XBOX_CONTROLLER_PORT);
  private Joystick joystick = new Joystick(DriverControlConsts.JOYSTICK_PORT);

  SwerveAutoBuilder autoBuilder; 
  HashMap<String, Command> eventMap = new HashMap<>();

  //AUTONOMOUS CHOICES 
  private Command highMobility = new HighMobility(swerveSubsystem, clawSubsystem, pivotSubsystem, elevatorSubsystem);
  private Command highBal = new HighBal(swerveSubsystem, clawSubsystem, pivotSubsystem, elevatorSubsystem);
  private Command high = new High(swerveSubsystem, clawSubsystem, pivotSubsystem, elevatorSubsystem);
  private Command doNothing = new DoNothing();
  private Command redHighBalEnc = new RedHighBalEnc(swerveSubsystem, clawSubsystem, pivotSubsystem, elevatorSubsystem); 
  private Command blueHighBalEnc = new BlueHighBalEnc(swerveSubsystem, clawSubsystem, pivotSubsystem, elevatorSubsystem);
  private Command mixedBalance = new MixedBalance(swerveSubsystem);
  private Command swerveLock = new Lock(swerveSubsystem);
  private Command linePath; 
  private Command S_Path; 
  public SendableChooser<Command> autoChooser = new SendableChooser<>();

  public RobotContainer() {
    swerveSubsystem.setDefaultCommand(new FieldOriented(swerveSubsystem,
        () -> xbox.getLeftY() * 0.95,
        () -> xbox.getLeftX() * 0.95,
        () -> -xbox.getRightX() * 0.95));

    selectAuto();
    configureBindings();
  }


  private void configureBindings() {

    /* SWERVE */

    // NORMAN SAID NO NEED
    /*new JoystickButton(xbox, 1).toggleOnTrue(
        new FieldOriented(swerveSubsystem,
            () -> xbox.getLeftY() * 0.35,
            () -> xbox.getLeftX() * 0.35,
            () -> -xbox.getRightX() * 0.35));
    new JoystickButton(xbox, 6).toggleOnTrue(
        new DriverControl(swerveSubsystem,
            () -> -xbox.getLeftY() * 0.75,
            () -> -xbox.getLeftX() * 0.75,
            () -> -xbox.getRightX() * 0.75));
    */
    new JoystickButton(xbox, 2).toggleOnTrue(new Lock(swerveSubsystem));
    // new JoystickButton(xbox, 4).toggleOnTrue(new Endgame(swerveSubsystem, () -> xbox.getLeftY()));
    new JoystickButton(xbox, 4).toggleOnTrue(new TankEndgame(swerveSubsystem, () -> xbox.getLeftY(), () -> -xbox.getRightY()));

    // FOR TESTING
    new JoystickButton(xbox, 7).onTrue(new InstantCommand(() -> swerveSubsystem.resetNavx()));
    new JoystickButton(xbox, 3).onTrue(new LandingGearIn(swerveSubsystem));

    /* CLAW */
    new JoystickButton(xbox, 5).onTrue(new Claw(clawSubsystem));

    new JoystickButton(joystick, 8).onTrue(new Go90Clockwise(clawSubsystem));
    new JoystickButton(joystick, 10).onTrue(new ToStartingPosition(clawSubsystem));
    new JoystickButton(joystick, 12).onTrue(new Go90Counterclockwise(clawSubsystem));

    new JoystickButton(joystick, 2).whileTrue(new ManualClaw(clawSubsystem, () -> joystick.getX()));

    /* PIVOT */
    new JoystickButton(joystick, 11).onTrue(new LowPickUp(pivotSubsystem, elevatorSubsystem));
    new JoystickButton(joystick, 9).onTrue(new ParallelCommandGroup(new PivotMiddleCommand(pivotSubsystem), new MidPosition(elevatorSubsystem)));
    new JoystickButton(joystick, 7).onTrue(new TopNode(pivotSubsystem, elevatorSubsystem));
    new JoystickButton(joystick, 5).onTrue(Tucked.getCommand(pivotSubsystem, elevatorSubsystem, clawSubsystem));
    new JoystickButton(joystick, 1).whileTrue(new PivotJoystickCommand(pivotSubsystem, () -> -joystick.getY()));

    /* ELEVATOR */
    new POVButton(joystick, 0).whileTrue(new ManualElevatorDrive(elevatorSubsystem, 0.75));
    new POVButton(joystick, 180).whileTrue(new ManualElevatorDrive(elevatorSubsystem, -0.75));

    // new JoystickButton(joystick, 3).onTrue(new LowPosition(elevatorSubsystem));

    /* LIGHTS */
    new JoystickButton(joystick, 6).toggleOnTrue(new Yellow(lights));
    new JoystickButton(joystick, 4).toggleOnTrue(new Violet(lights));

    /* PATH PLANNER */
    generatePathPlannerGroups();
    createAutoBuilder();

  }

  public Command getAutonomousCommand() {
    return autoChooser.getSelected();
  }

  /* AUTO BUILDER */
  //create the AutoBuilder obj and event map 
  private void createAutoBuilder() {
    //create event map 
    HashMap<String, Command> eventMap = new HashMap<>();

    //adding event markers to the eventMap
    eventMap.put("Marker 1", new InstantCommand(() -> SmartDashboard.putString("Auto Marker", "1")));
    eventMap.put("Marker 2", new InstantCommand(() -> SmartDashboard.putString("Auto Marker", "2")));

    //uses calling by reference 
    //PIDs are currently 0
    autoBuilder = new SwerveAutoBuilder(
      swerveSubsystem::getPose, //Pose2d 
      swerveSubsystem::resetOdometry, //reset Pose 
      SwerveConsts.DRIVE_KINEMATICS, //drive translations (kinematics)
      new PIDConstants(SwerveConsts.kP_XY, 0, 0), //translational PID 
      new PIDConstants(SwerveConsts.kP_THETA, 0, 0), //rotational PID 
      swerveSubsystem::setModuleStates, //swerve states 
      eventMap);
  }

  /* PATH PLANNER TRAJECTORIES */
  //generate PathPlanner groups and add them to the auto chooser 
  private void generatePathPlannerGroups() {
    //PATH CONSTRAINTS ARE NOT CORRECT 
    //loads paths from PP and generates trajectories 
    List<PathPlannerTrajectory> testLinePath = PathPlanner.loadPathGroup("LinePath", new PathConstraints(2, 2));  
    List<PathPlannerTrajectory> testSPath = PathPlanner.loadPathGroup("S_Path", new PathConstraints(2, 2)); 

    //translating the PathPlannerTrajectories into Commands 
    linePath = autoBuilder.fullAuto(testLinePath);
    S_Path = autoBuilder.fullAuto(testSPath);

  }

  public Command getSwerveLock(){
    return swerveLock;
  }

  public void selectAuto() {
    autoChooser.setDefaultOption("Do Nothing", doNothing);
    autoChooser.addOption("High Mobility", highMobility);
    autoChooser.addOption("High Balance", highBal);
    autoChooser.addOption("Red High Balance", redHighBalEnc);
    autoChooser.addOption("Blue High Balance", blueHighBalEnc);
    autoChooser.addOption("High ONLY", high);
    autoChooser.addOption("Mixed Balance ONLY", mixedBalance);
    autoChooser.addOption("Line Path", linePath);
    autoChooser.addOption("S Path", S_Path);

    SmartDashboard.putData(autoChooser);
  }

}
