package controller;

import model.entity.Person;
import model.entity.PersonException;
import model.dao.DAOArrayList;
import model.dao.DAOFile;
import model.dao.DAOFileSerializable;
import model.dao.DAOHashMap;
import model.dao.DAOSQL;
import model.dao.IDAO;
import start.Routes;
import view.DataStorageSelection;
import view.Delete;
import view.Insert;
import view.Menu;
import view.Read;
import view.ReadAll;
import view.Update;
import view.Count;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.persistence.*;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import org.jdatepicker.DateModel;
import utils.Constants;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import model.dao.DAOJPA;
import view.Login;

/**
 * This class starts the visual part of the application and programs and manages
 * all the events that it can receive from it. For each event received the
 * controller performs an action.
 *
 * @author Francesc Perez
 * @version 1.1.0
 */
public class ControllerImplementation implements IController, ActionListener {

    //Instance variables used so that both the visual and model parts can be 
    //accessed from the Controller.
    private final DataStorageSelection dSS;
    private IDAO dao;
    private Menu menu;
    private Insert insert;
    private Read read;
    private Delete delete;
    private Update update;
    private ReadAll readAll;
    private Count count;
    private Login login;
    private String userRole;

    /**
     * This constructor allows the controller to know which data storage option
     * the user has chosen.Schedule an event to deploy when the user has made
     * the selection.
     *
     * @param dSS
     */
    public ControllerImplementation(DataStorageSelection dSS) {
        this.dSS = dSS;
        ((JButton) (dSS.getAccept()[0])).addActionListener(this);
    }

    /**
     * With this method, the application is started, asking the user for the
     * chosen storage system.
     */
    @Override
    public void start() {
        dSS.setVisible(true);
    }

    /**
     * This receives method handles the events of the visual part. Each event
     * has an associated action.
     *
     * @param e The event generated in the visual part
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == dSS.getAccept()[0]) {
            handleDataStorageSelection();
        } else if (e.getSource() == login.getLogin()) {
            handleLoginAction();
        } else if (e.getSource() == menu.getInsert()) {
            handleInsertAction();
        } else if (insert != null && e.getSource() == insert.getInsert()) {
            handleInsertPerson();
        } else if (e.getSource() == menu.getRead()) {
            handleReadAction();
        } else if (read != null && e.getSource() == read.getRead()) {
            handleReadPerson();
        } else if (e.getSource() == menu.getDelete()) {
            handleDeleteAction();
        } else if (delete != null && e.getSource() == delete.getDelete()) {
            handleDeletePerson();
        } else if (e.getSource() == menu.getUpdate()) {
            handleUpdateAction();
        } else if (update != null && e.getSource() == update.getRead()) {
            handleReadForUpdate();
        } else if (update != null && e.getSource() == update.getUpdate()) {
            handleUpdatePerson();
        } else if (e.getSource() == menu.getReadAll()) {
            handleReadAll();
        } else if (e.getSource() == menu.getDeleteAll()) {
            handleDeleteAll();
        } else if (e.getSource() == menu.getCount()) {
            handleCount();
        }
    }

    public void exportDataToCSV() {
        try {
            ArrayList<Person> allPeople = dao.readAll();
            if (allPeople != null && !allPeople.isEmpty()) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save as CSV");

                // Sugerir un nombre de archivo por defecto
                LocalDate now = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                String defaultFileName = "people_data_" + now.format(formatter) + ".csv";
                fileChooser.setSelectedFile(new File(defaultFileName));

                int userSelection = fileChooser.showSaveDialog(readAll);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    String filePath = fileToSave.getAbsolutePath();

                    // Asegurar extensión .csv
                    if (!filePath.toLowerCase().endsWith(".csv")) {
                        filePath += ".csv";
                        fileToSave = new File(filePath);
                    }

                    try (PrintWriter writer = new PrintWriter(new FileWriter(fileToSave))) {
                        // Escribir encabezados
                        writer.println("NIF,Name,Date of Birth,Photo");

                        // Escribir datos
                        for (Person person : allPeople) {
                            String dob = person.getDateOfBirth() != null
                                    ? person.getDateOfBirth().toString() : "";
                            String photo = person.getPhoto() != null ? "yes" : "no";

                            // Escapar comas en los datos
                            String name = person.getName().contains(",")
                                    ? "\"" + person.getName() + "\"" : person.getName();

                            writer.println(String.join(",",
                                    person.getNif(),
                                    name,
                                    dob,
                                    photo
                            ));
                        }

                        JOptionPane.showMessageDialog(readAll,
                                "Data successfully exported to:\n" + fileToSave.getAbsolutePath(),
                                "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(readAll,
                                "Error exporting to CSV:\n" + ex.getMessage(),
                                "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(readAll,
                        "No data available to export.",
                        "Export", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(readAll,
                    "Error getting data for export:\n" + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void handleDataStorageSelection() {
        String daoSelected = ((javax.swing.JCheckBox) (dSS.getAccept()[1])).getText();
        dSS.dispose();
        switch (daoSelected) {
            case Constants.STORAGE_ARRAYLIST:
                dao = new DAOArrayList();
                break;
            case Constants.STORAGE_HASHMAP:
                dao = new DAOHashMap();
                break;
            case Constants.STORAGE_FILE:
                setupFileStorage();
                break;
            case Constants.STORAGE_FILE_SERIALIZATION:
                setupFileSerialization();
                break;
            case Constants.STORAGE_SQL:
                setupSQLDatabase();
                break;
            case Constants.STORAGE_JPA:
                setupJPADatabase();
                break;
        }
        setupUsers();
        handleLogin();
    }

    private void setupFileStorage() {
        File folderPath = new File(Routes.FILE.getFolderPath());
        File folderPhotos = new File(Routes.FILE.getFolderPhotos());
        File dataFile = new File(Routes.FILE.getDataFile());
        folderPath.mkdir();
        folderPhotos.mkdir();
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dSS, "File structure not created. Closing application.", "File - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        dao = new DAOFile();
    }

    private void setupFileSerialization() {
        File folderPath = new File(Routes.FILES.getFolderPath());
        File dataFile = new File(Routes.FILES.getDataFile());
        folderPath.mkdir();
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dSS, "File structure not created. Closing application.", "FileSer - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        dao = new DAOFileSerializable();
    }

    private void setupSQLDatabase() {
        try {
            Connection conn = DriverManager.getConnection(
                    Routes.DB.getDbServerAddress() + Routes.DB.getDbServerComOpt(),
                    Routes.DB.getDbServerUser(),
                    Routes.DB.getDbServerPassword());

            if (conn != null) {
                Statement stmt = conn.createStatement();

                // Crear base de datos si no existe
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + Routes.DB.getDbServerDB() + ";");

                // Crear tabla para personas (tu tabla existente)
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Routes.DB.getDbServerDB() + "." + Routes.DB.getDbServerTABLE() + "("
                        + "nif VARCHAR(9) PRIMARY KEY NOT NULL, "
                        + "name VARCHAR(50), "
                        + "dateOfBirth DATE, "
                        + "photo VARCHAR(200));");
                stmt.close();
                conn.close();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(dSS, "SQL-DDBB structure not created. Closing application.",
                    "SQL_DDBB - People v1.1.0", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        dao = new DAOSQL();
    }

    private void setupUsers() {
        try {
            Connection conn = DriverManager.getConnection(
                    Routes.DB.getDbServerAddress() + Routes.DB.getDbServerComOpt(),
                    Routes.DB.getDbServerUser(),
                    Routes.DB.getDbServerPassword());

            if (conn != null) {
                Statement stmt = conn.createStatement();

                // Crear base de datos si no existe
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + Routes.DB.getDbServerDB() + ";");

                // Crear tabla para usuarios y roles
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Routes.DB.getDbServerDB() + ".users ("
                        + "username VARCHAR(50) PRIMARY KEY, "
                        + "password VARCHAR(100) NOT NULL, "
                        + "user_role VARCHAR(20) NOT NULL DEFAULT 'employee');");

                // Insertar usuario admin por defecto (password debería ser hasheado en producción)
                try {
                    stmt.executeUpdate("INSERT INTO " + Routes.DB.getDbServerDB() + ".users VALUES "
                            + "('admin', 'admin123', 'admin'), "
                            + "('employee1', 'emp123', 'employee');");
                } catch (SQLException e) {
                }

                stmt.close();
                conn.close();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(dSS, "SQL-DDBB structure not created, unable to get valid user. Closing application.",
                    "SQL_DDBB - People v1.1.0", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void setupJPADatabase() {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory(Routes.DBO.getDbServerAddress());
            EntityManager em = emf.createEntityManager();
            em.close();
            emf.close();
        } catch (PersistenceException ex) {
            JOptionPane.showMessageDialog(dSS, "JPA_DDBB not created. Closing application.", "JPA_DDBB - People v1.1.0", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        dao = new DAOJPA();
    }

    private void setupMenu() {
        menu = new Menu();
        menu.setVisible(true);
        menu.getInsert().addActionListener(this);
        menu.getRead().addActionListener(this);
        menu.getUpdate().addActionListener(this);
        menu.getDelete().addActionListener(this);
        menu.getReadAll().addActionListener(this);
        menu.getDeleteAll().addActionListener(this);
        menu.getCount().addActionListener(this);
    }

    private void handleInsertAction() {
        insert = new Insert(menu, true);
        insert.getInsert().addActionListener(this);
        insert.setVisible(true);
    }

    private void handleInsertPerson() {
        Person p = new Person(insert.getNam().getText(), insert.getNif().getText());
        if (insert.getDateOfBirth().getModel().getValue() != null) {
            p.setDateOfBirth(((GregorianCalendar) insert.getDateOfBirth().getModel().getValue()).getTime());
        }
        if (insert.getPhoto().getIcon() != null) {
            p.setPhoto((ImageIcon) insert.getPhoto().getIcon());
        }
        if (insert.getEmail().getText() != null) {
            p.setEmail(insert.getEmail().getText().trim());
        }
        if (insert.getPhone().getText() != null) {
            p.setPhoneNumber(insert.getPhone().getText().trim());
        }
        if (insert.getPostal().getText() != null) {
            p.setPostal(insert.getPostal().getText().trim());
        }
        insert(p);
        insert.getReset().doClick();

    }

    private void handleReadAction() {
        read = new Read(menu, true);
        read.getRead().addActionListener(this);
        read.setVisible(true);
    }

    private void handleReadPerson() {
        Person p = new Person(read.getNif().getText());
        Person pNew = read(p);
        if (pNew != null) {
            read.getNam().setText(pNew.getName());
            if (pNew.getDateOfBirth() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(pNew.getDateOfBirth());
                DateModel<Calendar> dateModel = (DateModel<Calendar>) read.getDateOfBirth().getModel();
                dateModel.setValue(calendar);
            }
            read.getEmail().setText(pNew.getEmail());
            read.getPhoneNumber().setText(pNew.getPhoneNumber());
            read.getPostal().setText(pNew.getPostal());
            //To avoid charging former images
            if (pNew.getPhoto() != null) {
                pNew.getPhoto().getImage().flush();
                read.getPhoto().setIcon(pNew.getPhoto());
            }
        } else {
            JOptionPane.showMessageDialog(read, p.getNif() + " doesn't exist.", read.getTitle(), JOptionPane.WARNING_MESSAGE);
            read.getReset().doClick();
        }
    }

    public void handleDeleteAction() {
        delete = new Delete(menu, true);
        delete.getDelete().addActionListener(this);
        delete.setVisible(true);
    }

    public void handleDeletePerson() {
        if (delete != null) {
            Person p = new Person(delete.getNif().getText());
            delete(p);
            delete.getReset().doClick();
        }
    }

    public void handleUpdateAction() {
        update = new Update(menu, true);
        update.getUpdate().addActionListener(this);
        update.getRead().addActionListener(this);
        update.setVisible(true);
        update.setEnabled(false);
    }

    public void handleReadForUpdate() {
        if (update != null) {
            Person p = new Person(update.getNif().getText());
            Person pNew = read(p);
            if (pNew != null) {
                update.getNam().setEnabled(true);
                update.getDateOfBirth().setEnabled(true);
                update.getEmail().setEnabled(true);
                update.getPhone().setEnabled(true);
                update.getPostal().setEnabled(true);
                update.getNam().setText(pNew.getName());
                update.getEmail().setText(pNew.getEmail());
                update.getPhone().setText(pNew.getPhoneNumber());
                update.getPostal().setText(pNew.getPostal());
                if (pNew.getDateOfBirth() != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(pNew.getDateOfBirth());
                    DateModel<Calendar> dateModel = (DateModel<Calendar>) update.getDateOfBirth().getModel();
                    dateModel.setValue(calendar);
                }
                if (pNew.getPhoto() != null) {
                    pNew.getPhoto().getImage().flush();
                    update.getPhoto().setIcon(pNew.getPhoto());
                }
            } else {
                JOptionPane.showMessageDialog(update, p.getNif() + " doesn't exist.", update.getTitle(), JOptionPane.WARNING_MESSAGE);
                update.getReset().doClick();
            }
        }
    }

    public void handleUpdatePerson() {
        if (update != null) {
            Person p = new Person(update.getNam().getText(), update.getNif().getText());
            if ((update.getDateOfBirth().getModel().getValue()) != null) {
                p.setDateOfBirth(((GregorianCalendar) update.getDateOfBirth().getModel().getValue()).getTime());
            }
            if ((update.getEmail().getText()) != null) {
                p.setEmail(update.getEmail().getText());
            }
            if ((update.getPhone().getText()) != null) {
                p.setPhoneNumber(update.getPhone().getText());
            }
            if ((update.getPostal().getText()) != null) {
                p.setPostal(update.getPostal().getText());
            }
            if ((ImageIcon) (update.getPhoto().getIcon()) != null) {
                p.setPhoto((ImageIcon) update.getPhoto().getIcon());
            }
            update(p);
            update.getReset().doClick();
        }
    }

    public void handleReadAll() {
        ArrayList<Person> s = readAll();
        if (s.isEmpty()) {
            JOptionPane.showMessageDialog(menu, "There are not people registered yet.",
                    "Read All - People v1.1.0", JOptionPane.WARNING_MESSAGE);
        } else {
            // Create the readAll dialog only if it doesn't exist
            if (readAll == null) {
                readAll = new ReadAll(menu, true);
                // Connect the export button's action listener only once
                readAll.getExportButton().addActionListener(e -> exportDataToCSV());
            }

            // Clear the existing table data to avoid duplicates
            DefaultTableModel model = (DefaultTableModel) readAll.getTable().getModel();
            model.setRowCount(0); // Clear previous rows

            // Populate the table with new data
            for (int i = 0; i < s.size(); i++) {
                model.addRow(new Object[6]); // Match the number of columns
                model.setValueAt(s.get(i).getNif(), i, 0);
                model.setValueAt(s.get(i).getName(), i, 1);
                model.setValueAt(s.get(i).getDateOfBirth() != null ? s.get(i).getDateOfBirth().toString() : "", i, 2);
                model.setValueAt(s.get(i).getEmail(), i, 3);
                model.setValueAt(s.get(i).getPhoneNumber(), i, 4);
                model.setValueAt(s.get(i).getPostal(), i, 5);
                model.setValueAt(s.get(i).getPhoto() != null ? "yes" : "no", i, 6);
            }

            // Show the dialog
            readAll.setVisible(true);
        }
    }

    public void handleDeleteAll() {
        Object[] options = {"Yes", "No"};
        //int answer = JOptionPane.showConfirmDialog(menu, "Are you sure to delete all people registered?", "Delete All - People v1.1.0", 0, 0);
        int answer = JOptionPane.showOptionDialog(
                menu,
                "Are you sure you want to delete all registered people?",
                "Delete All - People v1.1.0",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1] // Default selection is "No"
        );

        if (answer == 0) {
            deleteAll();
            JOptionPane.showMessageDialog(menu, "All persons have been deleted successfully!", "Delete All - People v1.1.0", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void handleCount() {
        count = new Count(menu, true);
        int totalPeople = count(); // Get the total number of people
        count.getCount().setText(String.valueOf(totalPeople)); // Set count in JTextField
        count.setVisible(true);
    }

    public void handleLogin() {
        login = new Login(menu, true);
        login.getLogin().addActionListener(this);
        login.setVisible(true);
    }

    private void handleLoginAction() {
        if (loginSuccessfully()) {
            login.dispose();
            setupMenu(); // Call setupMenu after successful login
            applyRolePermissions(); // New method to apply permissions based on role
        } else {
            JOptionPane.showMessageDialog(login, "Invalid username or password.", "Login - People v1.1.0", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private boolean loginSuccessfully() {
        String username = login.getUserName().getText();
        char[] passwordChars = login.getPassword().getPassword();
        String password = new String(passwordChars);

        try {
            Connection conn = DriverManager.getConnection(
                    Routes.DB.getDbServerAddress() + Routes.DB.getDbServerComOpt(),
                    Routes.DB.getDbServerUser(),
                    Routes.DB.getDbServerPassword());

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT user_role FROM " + Routes.DB.getDbServerDB() + ".users "
                    + "WHERE username = ? AND password = ?");

            stmt.setString(1, username);
            stmt.setString(2, password); // In a real application, hash the password

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                this.userRole = rs.getString("user_role"); // Store the user role
                rs.close();
                stmt.close();
                conn.close();
                java.util.Arrays.fill(passwordChars, ' '); // Clear password in memory
                return true;
            }

            rs.close();
            stmt.close();
            conn.close();
            java.util.Arrays.fill(passwordChars, ' ');
            return false;
        } catch (SQLException ex) {
            java.util.Arrays.fill(passwordChars, ' ');
            JOptionPane.showMessageDialog(login, "Error de conexión: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * This function inserts the Person object with the requested NIF, if it
     * doesn't exist. If there is any access problem with the storage device,
     * the program stops.
     *
     * @param p Person to insert
     */
    @Override
    public void insert(Person p) {
        try {
            if (dao.read(p) == null) {
                dao.insert(p);
                JOptionPane.showMessageDialog(insert, "Person inserted successfully!", "Insert", JOptionPane.INFORMATION_MESSAGE);
            } else {
                throw new PersonException(p.getNif() + " is registered and can not "
                        + "be INSERTED.");
            }
        } catch (Exception ex) {
            //Exceptions generated by file read/write access. If something goes 
            // wrong the application closes.
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(insert, ex.getMessage() + ex.getClass() + " Closing application.", insert.getTitle(), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            if (ex instanceof PersonException) {
                JOptionPane.showMessageDialog(insert, ex.getMessage(), insert.getTitle(), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * This function updates the Person object with the requested NIF, if it
     * doesn't exist. NIF can not be aupdated. If there is any access problem
     * with the storage device, the program stops.
     *
     * @param p Person to update
     */
    @Override
    public void update(Person p) {
        try {
            dao.update(p);
            JOptionPane.showMessageDialog(update, "Person updated successfully!", "Update", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            //Exceptions generated by file read/write access. If something goes 
            // wrong the application closes.
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(update, ex.getMessage() + ex.getClass() + " Closing application.", update.getTitle(), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    /**
     * This function deletes the Person object with the requested NIF, if it
     * exists. If there is any access problem with the storage device, the
     * program stops.
     *
     * @param p Person to read
     */
    @Override
    public void delete(Person p) {
        try {
            if (dao.read(p) != null) {
                int respuesta = JOptionPane.showConfirmDialog(null, "¿Deseas continuar?", "Confirmación", JOptionPane.YES_NO_OPTION);
                if (respuesta == JOptionPane.YES_OPTION) {
                    dao.delete(p);
                    JOptionPane.showMessageDialog(null, "Succesfully Deleted");
                } else if (respuesta == JOptionPane.NO_OPTION) {
                    JOptionPane.showMessageDialog(null, "Not Deleted");
                }
            } else {
                throw new PersonException(p.getNif() + " is not registered and can not "
                        + "be DELETED");
            }
        } catch (Exception ex) {
            //Exceptions generated by file, DDBB read/write access. If something  
            //goes wrong the application closes.
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(read, ex.getMessage() + ex.getClass() + " Closing application.", "Insert - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            if (ex instanceof PersonException) {
                JOptionPane.showMessageDialog(read, ex.getMessage(), "Delete - People v1.1.0", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * This function returns the Person object with the requested NIF, if it
     * exists. Otherwise it returns null. If there is any access problem with
     * the storage device, the program stops.
     *
     * @param p Person to read
     * @return Person or null
     */
    @Override
    public Person read(Person p) {
        try {
            Person pTR = dao.read(p);
            if (pTR != null) {
                return pTR;
            }
        } catch (Exception ex) {

            //Exceptions generated by file read access. If something goes wrong 
            //reading the file, the application closes.
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(read, ex.getMessage() + " Closing application.", read.getTitle(), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        return null;
    }

    /**
     * This function returns the people registered. If there is any access
     * problem with the storage device, the program stops.
     *
     * @return ArrayList
     */
    @Override
    public ArrayList<Person> readAll() {
        ArrayList<Person> people = new ArrayList<>();
        try {
            people = dao.readAll();
        } catch (Exception ex) {
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(readAll, ex.getMessage() + " Closing application.", readAll.getTitle(), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        return people;
    }

    /**
     * This function deletes all the people registered. If there is any access
     * problem with the storage device, the program stops.
     */
    @Override
    public void deleteAll() {
        try {
            dao.deleteAll();
        } catch (Exception ex) {
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(menu, ex.getMessage() + " Closing application.", "Delete All - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    /**
     * This function returns the total number of people registered in the
     * system. If there is any access problem with the storage device, the
     * program stops.
     *
     * @return the number of registered Person entries
     */
    @Override
    public int count() {
        int count = 0;
        try {
            count = dao.count();
        } catch (Exception ex) {
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(menu, ex.getMessage() + " " + ex.getClass() + " Closing application.", "Count - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        return count;
    }

    private void applyRolePermissions() {
        if (userRole != null) {
            if ("employee".equalsIgnoreCase(userRole)) {
                // Disable actions for employee
                menu.getInsert().setEnabled(false);
                menu.getUpdate().setEnabled(false);
                menu.getDelete().setEnabled(false);
                menu.getDeleteAll().setEnabled(false);
                menu.getInsert().setVisible(false);
                menu.getUpdate().setVisible(false);
                menu.getDelete().setVisible(false);
                menu.getDeleteAll().setVisible(false);
            } else if ("admin".equalsIgnoreCase(userRole)) {
                // Ensure all actions are enabled for admin (default behavior, but good to be explicit)
                menu.getInsert().setEnabled(true);
                menu.getUpdate().setEnabled(true);
                menu.getDelete().setEnabled(true);
                menu.getDeleteAll().setEnabled(true);
                menu.getInsert().setVisible(true);
                menu.getUpdate().setVisible(true);
                menu.getDelete().setVisible(true);
                menu.getDeleteAll().setVisible(true);
            }
        }
    }
}
